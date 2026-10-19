package ec.mdd.plataforma.errores;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.pruebas.ServidorJwks;
import ec.mdd.plataforma.pruebas.TokensDePrueba;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * La traducción de errores de un proceso con un servidor real, la seguridad de los procesos y un
 * token válido (escenarios «Error reconocido», «Error técnico inesperado» y «Error de entrada»;
 * ADR-016).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class ManejadorDeErroresTest {

    private static final TokensDePrueba TOKENS = new TokensDePrueba();
    private static final ServidorJwks JWKS = arrancarJwks();
    private static final String TOKEN = TOKENS.valido("autor.prueba", List.of("DOCENTE"));
    private static final List<String> CAMPOS_DEL_FRAMEWORK =
            List.of("\"timestamp\"", "\"path\"", "\"trace\"", "\"status\"", "\"error\"");

    private final HttpClient cliente = HttpClient.newHttpClient();

    @LocalServerPort private int puerto;

    @DynamicPropertySource
    static void jwks(DynamicPropertyRegistry registro) {
        registro.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", JWKS::uri);
    }

    @AfterAll
    static void cerrar() {
        JWKS.close();
    }

    @Test
    void dadoUnErrorDeDominioRegistrado_cuandoUnCasoDeUsoLoProduce_entoncesRespondeSuCodigo()
            throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/prueba/horas", null, null, null);

        assertThat(respuesta.statusCode()).isEqualTo(409);
        assertThat(respuesta.body())
                .isEqualTo(
                        "{\"titulo\":\"Horas excedidas\",\"descripcion\":\"La asignación supera las"
                                + " horas del docente. Quita horas y vuelve a intentarlo.\","
                                + "\"codigo\":\"PRUEBA_HORAS_EXCEDIDAS\"}");
        assertThat(respuesta.headers().firstValue("Content-Type")).hasValue("application/json");
    }

    @Test
    void dadoUnParametroDeRutaQueNoCumpleSuPatron_cuandoSeValida_entonces400ConSuCampo()
            throws Exception {
        HttpResponse<String> respuesta =
                pedir("GET", "/api/prueba/periodos/2025-x", null, null, null);

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(respuesta.body())
                .isEqualTo(
                        "{\"titulo\":\"Solicitud inválida\",\"descripcion\":\"Revisa los campos"
                                + " marcados y vuelve a intentarlo.\",\"codigo\":"
                                + "\"COMUN_SOLICITUD_INVALIDA\",\"detalles\":[{\"campo\":"
                                + "\"periodo\",\"descripcion\":\"no tiene el formato esperado\"}]}");
    }

    @Test
    void dadoUnaReglaDeEntradaQueElContratoNoExpresa_cuandoFalla_entonces400ConElDetalle()
            throws Exception {
        HttpResponse<String> respuesta =
                pedir("GET", "/api/prueba/regla-de-entrada", null, null, null);

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(respuesta.body())
                .isEqualTo(
                        "{\"titulo\":\"Solicitud inválida\",\"descripcion\":\"Revisa los campos"
                                + " marcados y vuelve a intentarlo.\",\"codigo\":"
                                + "\"COMUN_SOLICITUD_INVALIDA\",\"detalles\":[{\"campo\":"
                                + "\"permisos\",\"descripcion\":\"«Cargas de datos» no admite el"
                                + " permiso de firma.\"}]}");
    }

    @Test
    void dadoUnaSubclaseDeUnErrorRegistrado_cuandoSeProduce_entoncesUsaElCodigoDeLaSuperclase()
            throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/prueba/feriado", null, null, null);

        assertThat(respuesta.statusCode()).isEqualTo(409);
        assertThat(respuesta.body()).contains("PRUEBA_HORAS_EXCEDIDAS");
    }

    @Test
    void dadoUnaEntradaConEncabezados_cuandoSeProduceSuError_entoncesLosAgregaALaRespuesta()
            throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/prueba/espera", null, null, null);

        assertThat(respuesta.statusCode()).isEqualTo(429);
        assertThat(respuesta.headers().firstValue("Retry-After")).hasValue("42");
        assertThat(respuesta.body()).contains("\"codigo\":\"PRUEBA_ESPERA\"");
    }

    @Test
    void dadoUnErrorDeDominioSinRegistrar_cuandoSeProduce_entonces500YElRegistroLoAvisa(
            CapturedOutput salida) throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/prueba/sin-catalogo", null, null, null);

        assertThat(respuesta.statusCode()).isEqualTo(500);
        assertThat(respuesta.body())
                .contains("COMUN_ERROR_INTERNO")
                .doesNotContain("ReglaSinCatalogo");
        assertThat(salida.getOut()).contains("sin código en el catálogo");
    }

    @Test
    void dadoUnErrorTecnico_cuandoOcurre_entonces500ConIncidenteSinDetallesYRegistroSaneado(
            CapturedOutput salida) throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/prueba/tecnico", null, null, null);

        assertThat(respuesta.statusCode()).isEqualTo(500);
        assertThat(respuesta.body())
                .contains("\"codigo\":\"COMUN_ERROR_INTERNO\"")
                .doesNotContain("IllegalStateException")
                .doesNotContain("ec.mdd")
                .doesNotContain("eyJ")
                .doesNotContain("secreta");
        Matcher id =
                Pattern.compile("\"idIncidente\":\"([0-9a-f-]{36})\"").matcher(respuesta.body());
        assertThat(id.find()).isTrue();
        assertThat(salida.getOut())
                .contains("Incidente " + id.group(1))
                .contains("java.lang.IllegalStateException")
                .contains("at ec.mdd.plataforma.errores.AplicacionDePrueba")
                .contains("[oculto]")
                .doesNotContain("eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ4In0.firma")
                .doesNotContain("clave=secreta");
        assertSinCamposDelFramework(respuesta.body());
    }

    @Test
    void dadoDosErroresTecnicos_cuandoOcurren_entoncesTienenIncidentesDistintos() throws Exception {
        String primero = pedir("GET", "/api/prueba/tecnico", null, null, null).body();
        String segundo = pedir("GET", "/api/prueba/tecnico", null, null, null).body();

        assertThat(primero).isNotEqualTo(segundo);
    }

    @Test
    void dadoVariosCamposInvalidos_cuandoLlegaLaSolicitud_entonces400ConUnDetallePorCampo()
            throws Exception {
        HttpResponse<String> respuesta =
                pedir(
                        "POST",
                        "/api/prueba/datos",
                        "application/json",
                        null,
                        "{\"nombre\":null,\"codigo\":\"ab\",\"clave\":\"ABC\"}");

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(respuesta.body())
                .contains("\"codigo\":\"COMUN_SOLICITUD_INVALIDA\"")
                .contains("{\"campo\":\"nombre\",\"descripcion\":\"es obligatorio\"}")
                .contains(
                        "{\"campo\":\"codigo\",\"descripcion\":\"debe tener entre 3 y 10 caracteres\"}")
                .contains("{\"campo\":\"clave\",\"descripcion\":\"no tiene el formato esperado\"}")
                .doesNotContain("idIncidente");
    }

    @Test
    void dadoUnCuerpoMalFormado_cuandoLlega_entonces400CuerpoIlegibleSinElMensajeDelAnalizador()
            throws Exception {
        HttpResponse<String> respuesta =
                pedir("POST", "/api/prueba/datos", "application/json", null, "{\"nombre\":");

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(respuesta.body())
                .contains("COMUN_CUERPO_ILEGIBLE")
                .doesNotContainIgnoringCase("jackson")
                .doesNotContainIgnoringCase("unexpected");
    }

    @Test
    void dadoErroresDelFramework_cuandoOcurren_entoncesUsanSusCodigosComunes() throws Exception {
        HttpResponse<String> noExiste = pedir("GET", "/api/prueba/no-existe", null, null, null);
        HttpResponse<String> metodo = pedir("DELETE", "/api/prueba/horas", null, null, null);
        HttpResponse<String> formato =
                pedir("GET", "/api/prueba/json", null, "application/xml", null);
        HttpResponse<String> tipo = pedir("POST", "/api/prueba/datos", "text/plain", null, "hola");

        assertThat(noExiste.statusCode()).isEqualTo(404);
        assertThat(noExiste.body()).contains("COMUN_RECURSO_NO_ENCONTRADO");
        assertThat(metodo.statusCode()).isEqualTo(405);
        assertThat(metodo.body()).contains("COMUN_METODO_NO_PERMITIDO");
        assertThat(formato.statusCode()).isEqualTo(406);
        assertThat(formato.body()).contains("COMUN_FORMATO_NO_ACEPTADO");
        assertThat(tipo.statusCode()).isEqualTo(415);
        assertThat(tipo.body()).contains("COMUN_TIPO_NO_SOPORTADO");
        for (HttpResponse<String> respuesta : List.of(noExiste, metodo, formato, tipo)) {
            assertSinCamposDelFramework(respuesta.body());
        }
    }

    @Test
    void dadoUnaExcepcionDeSeguridad_cuandoLaLanzaUnControlador_entoncesNoEsUn500()
            throws Exception {
        assertThat(pedir("GET", "/api/prueba/prohibido", null, null, null).statusCode())
                .isEqualTo(403);
    }

    private static void assertSinCamposDelFramework(String cuerpo) {
        for (String campo : CAMPOS_DEL_FRAMEWORK) {
            assertThat(cuerpo).doesNotContain(campo);
        }
    }

    private HttpResponse<String> pedir(
            String metodo, String ruta, String tipo, String acepta, String cuerpo)
            throws IOException, InterruptedException {
        HttpRequest.Builder peticion =
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                        .header("Authorization", "Bearer " + TOKEN)
                        .method(
                                metodo,
                                cuerpo == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(cuerpo));
        if (tipo != null) {
            peticion.header("Content-Type", tipo);
        }
        if (acepta != null) {
            peticion.header("Accept", acepta);
        }
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static ServidorJwks arrancarJwks() {
        try {
            return new ServidorJwks(TOKENS.jwks());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
