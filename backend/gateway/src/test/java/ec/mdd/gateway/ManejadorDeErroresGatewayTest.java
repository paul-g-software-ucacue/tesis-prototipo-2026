package ec.mdd.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.pruebas.ServidorJwks;
import ec.mdd.plataforma.pruebas.TokensDePrueba;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * El API Gateway responde sus propios errores con el modelo común (D-40, ADR-016): ruta sin
 * destino, proceso que no responde y el 401 sin cambios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ManejadorDeErroresGatewayTest {

    private static final TokensDePrueba TOKENS = new TokensDePrueba();
    private static final ServidorJwks JWKS = arrancarJwks();
    private static final int PUERTO_CERRADO = puertoCerrado();
    private static final String TOKEN = TOKENS.valido("autor.prueba", List.of("DOCENTE"));
    private static final List<String> CAMPOS_DEL_FRAMEWORK =
            List.of("\"timestamp\"", "\"path\"", "\"requestId\"", "\"error\"", "\"status\"");

    private final HttpClient cliente = HttpClient.newHttpClient();

    @LocalServerPort private int puerto;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", JWKS::uri);
        registro.add("MDD_URI_SALIDA", () -> "http://127.0.0.1:" + PUERTO_CERRADO);
    }

    @AfterAll
    static void cerrar() {
        JWKS.close();
    }

    @Test
    void dadoUnaRutaSinDestino_cuandoLlegaConToken_entonces404ConElModeloComun() throws Exception {
        HttpResponse<String> respuesta = pedir("/api/inexistente/x", TOKEN);

        assertThat(respuesta.statusCode()).isEqualTo(404);
        assertThat(respuesta.body()).isEqualTo(cuerpo(CodigosComunes.RECURSO_NO_ENCONTRADO));
        assertThat(respuesta.headers().firstValue("Content-Type")).hasValue("application/json");
    }

    @Test
    void dadoUnProcesoQueNoResponde_cuandoSeLeReenvia_entonces503SinLaDireccionInterna()
            throws Exception {
        HttpResponse<String> respuesta = pedir("/api/dashboard/sesion", TOKEN);

        assertThat(respuesta.statusCode()).isEqualTo(503);
        assertThat(respuesta.body())
                .isEqualTo(cuerpo(CodigosComunes.SERVICIO_NO_DISPONIBLE))
                .doesNotContain(String.valueOf(PUERTO_CERRADO))
                .doesNotContain("127.0.0.1");
        for (String campo : CAMPOS_DEL_FRAMEWORK) {
            assertThat(respuesta.body()).doesNotContain(campo);
        }
    }

    @Test
    void dadoUnaPeticionSinToken_cuandoLaRutaNoResponde_entoncesSigueSiendoEl401DeSeguridad()
            throws Exception {
        HttpResponse<String> respuesta = pedir("/api/dashboard/sesion", null);

        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(respuesta.body()).isEqualTo(cuerpo(CodigosComunes.SEGURIDAD_NO_AUTENTICADO));
    }

    private static String cuerpo(DefinicionDeCodigo codigo) {
        return "{\"titulo\":\""
                + codigo.titulo()
                + "\",\"descripcion\":\""
                + codigo.descripcion()
                + "\",\"codigo\":\""
                + codigo.codigo()
                + "\"}";
    }

    private HttpResponse<String> pedir(String ruta, String token)
            throws IOException, InterruptedException {
        HttpRequest.Builder peticion =
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).GET();
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static int puertoCerrado() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ServidorJwks arrancarJwks() {
        try {
            return new ServidorJwks(TOKENS.jwks());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
