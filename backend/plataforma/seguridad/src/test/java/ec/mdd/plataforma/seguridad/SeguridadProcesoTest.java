package ec.mdd.plataforma.seguridad;

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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * La seguridad de los procesos con un servidor real y un JWKS servido por la prueba: cada causa de
 * rechazo da el mismo 401, y un token válido llega con sus roles (escenarios «Petición sin token» y
 * «Token alterado o vencido», ADR-014).
 */
@SpringBootTest(
        classes = AplicacionDePrueba.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.endpoints.web.exposure.include=health",
            "mdd.seguridad.rutas-publicas[0]=POST /api/prueba/sesion"
        })
class SeguridadProcesoTest {

    private static final String CUERPO_401 =
            "{\"titulo\":\"Sesión no válida\",\"descripcion\":\"Inicia sesión de nuevo para"
                    + " continuar.\",\"codigo\":\"SEGURIDAD_NO_AUTENTICADO\"}";

    private static final TokensDePrueba TOKENS = new TokensDePrueba();
    private static final ServidorJwks JWKS = arrancarJwks();

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

    static List<String> autorizacionesInvalidas() {
        return List.of(
                "",
                "Basic dXN1YXJpbzpjbGF2ZQ==",
                "Bearer %%%",
                "Bearer no.es.un-token",
                "Bearer " + TOKENS.alterado(),
                "Bearer " + TOKENS.vencido(),
                "Bearer " + TOKENS.deOtraLlave(),
                "Bearer " + TOKENS.deOtroEmisor(),
                "Bearer " + TOKENS.hs256(),
                "Bearer " + TOKENS.sinAlgoritmo());
    }

    @ParameterizedTest
    @MethodSource("autorizacionesInvalidas")
    void dadoUnTokenAusenteOInvalido_cuandoLlegaAlProceso_entoncesResponde401SiempreIgual(
            String autorizacion) throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/prueba/sesion", autorizacion);

        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(respuesta.body()).isEqualTo(CUERPO_401);
        assertThat(respuesta.headers().firstValue("Content-Type")).hasValue("application/json");
        assertThat(respuesta.headers().allValues("WWW-Authenticate")).containsExactly("Bearer");
    }

    @Test
    void dadoUnTokenValido_cuandoLlegaAlProceso_entoncesObtieneElUsuarioYSusRoles()
            throws Exception {
        String token = TOKENS.valido("autor.prueba", List.of("DOCENTE", "COORDINADOR_CARRERA"));

        HttpResponse<String> respuesta = pedir("GET", "/api/prueba/sesion", "Bearer " + token);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body())
                .isEqualTo("autor.prueba|ROLE_COORDINADOR_CARRERA,ROLE_DOCENTE");
    }

    @Test
    void dadoUnaRutaPublicaConMetodo_cuandoSeLlamaSinToken_entoncesSoloEseMetodoPasa()
            throws Exception {
        assertThat(pedir("POST", "/api/prueba/sesion", "").statusCode()).isEqualTo(200);
        assertThat(pedir("GET", "/api/prueba/sesion", "").statusCode()).isEqualTo(401);
    }

    @Test
    void dadoLaSaludDelProceso_cuandoSeLlamaSinToken_entoncesResponde() throws Exception {
        assertThat(pedir("GET", "/actuator/health", "").statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> pedir(String metodo, String ruta, String autorizacion)
            throws IOException, InterruptedException {
        HttpRequest.Builder peticion =
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                        .method(metodo, HttpRequest.BodyPublishers.noBody());
        if (!autorizacion.isEmpty()) {
            peticion.header("Authorization", autorizacion);
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
