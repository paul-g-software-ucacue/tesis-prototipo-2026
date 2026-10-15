package ec.mdd.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import ec.mdd.plataforma.pruebas.ServidorJwks;
import ec.mdd.plataforma.pruebas.TokensDePrueba;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * El API Gateway rechaza con el 401 uniforme toda petición a la API sin un token válido, antes de
 * reenviarla, y reenvía un token válido sin cambiarlo (escenarios «Petición sin token» y «Token
 * alterado o vencido», ADR-014). Un proceso falso hace de destino de las rutas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeguridadGatewayTest {

    private static final String CUERPO_401 =
            "{\"titulo\":\"Sesión no válida\",\"descripcion\":\"Inicia sesión de nuevo para"
                    + " continuar.\",\"codigo\":\"SEGURIDAD_NO_AUTENTICADO\"}";

    private static final TokensDePrueba TOKENS = new TokensDePrueba();
    private static final ServidorJwks JWKS = arrancar(() -> new ServidorJwks(TOKENS.jwks()));
    private static final AtomicReference<String> AUTORIZACION_RECIBIDA = new AtomicReference<>();
    private static final HttpServer PROCESO = arrancar(SeguridadGatewayTest::procesoFalso);

    private final HttpClient cliente = HttpClient.newHttpClient();

    @LocalServerPort private int puerto;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        String proceso = "http://127.0.0.1:" + PROCESO.getAddress().getPort();
        registro.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", JWKS::uri);
        registro.add("MDD_URI_IDENTIDAD", () -> proceso);
        registro.add("MDD_URI_DATOS", () -> proceso);
    }

    @BeforeEach
    void limpiar() {
        AUTORIZACION_RECIBIDA.set(null);
    }

    @AfterAll
    static void cerrar() {
        JWKS.close();
        PROCESO.stop(0);
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
    void dadoUnTokenAusenteOInvalido_cuandoLlegaAlGateway_entoncesResponde401SinReenviar(
            String autorizacion) throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/cargas/sesion", autorizacion);

        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(respuesta.body()).isEqualTo(CUERPO_401);
        assertThat(respuesta.headers().firstValue("Content-Type")).hasValue("application/json");
        assertThat(respuesta.headers().allValues("WWW-Authenticate")).containsExactly("Bearer");
        assertThat(AUTORIZACION_RECIBIDA.get()).isNull();
    }

    @Test
    void dadoUnTokenValido_cuandoLlegaAlGateway_entoncesLoReenviaSinCambiarlo() throws Exception {
        String autorizacion = "Bearer " + TOKENS.valido("autor.prueba", List.of("DOCENTE"));

        HttpResponse<String> respuesta = pedir("GET", "/api/cargas/sesion", autorizacion);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(AUTORIZACION_RECIBIDA.get()).isEqualTo(autorizacion);
    }

    @Test
    void dadoElInicioDeSesion_cuandoSeLlamaSinToken_entoncesSeReenvia() throws Exception {
        assertThat(pedir("POST", "/api/usuarios/sesion", "").statusCode()).isEqualTo(200);
        assertThat(pedir("GET", "/api/usuarios/sesion", "").statusCode()).isEqualTo(401);
    }

    @Test
    void dadoUnaRutaEntreProcesos_cuandoLlegaAlGateway_entoncesNoSeReenviaConNingunToken()
            throws Exception {
        // ADR-018: el API Gateway no enruta /interno/**; su seguridad lo niega (401 o 403).
        HttpResponse<String> sinToken = pedir("POST", "/interno/usuarios/eventos", "");
        HttpResponse<String> conToken =
                pedir(
                        "POST",
                        "/interno/usuarios/eventos",
                        "Bearer " + TOKENS.valido("autor.prueba", List.of("DOCENTE")));
        HttpResponse<String> deProceso =
                pedir(
                        "POST",
                        "/interno/usuarios/token-proceso",
                        "Bearer " + TOKENS.valido("proceso:distributivo", List.of("PROCESO")));

        assertThat(sinToken.statusCode()).isEqualTo(401);
        assertThat(sinToken.body()).isEqualTo(CUERPO_401);
        assertThat(conToken.statusCode()).isEqualTo(403);
        assertThat(deProceso.statusCode()).isEqualTo(403);
        assertThat(AUTORIZACION_RECIBIDA.get()).isNull();
    }

    @Test
    void dadoLaSaludDelGateway_cuandoSeLlamaSinToken_entoncesResponde() throws Exception {
        assertThat(pedir("GET", "/actuator/health/readiness", "").statusCode()).isEqualTo(200);
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

    private static HttpServer procesoFalso() throws IOException {
        HttpServer servidor =
                HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        // Si el API Gateway reenviara una ruta entre procesos, el proceso falso la anotaría.
        servidor.createContext(
                "/interno",
                intercambio -> {
                    AUTORIZACION_RECIBIDA.set("reenviada a /interno");
                    intercambio.sendResponseHeaders(204, -1);
                    intercambio.close();
                });
        servidor.createContext(
                "/api",
                intercambio -> {
                    AUTORIZACION_RECIBIDA.set(
                            intercambio.getRequestHeaders().getFirst("Authorization"));
                    byte[] cuerpo = "{}".getBytes(StandardCharsets.UTF_8);
                    intercambio.getResponseHeaders().add("Content-Type", "application/json");
                    intercambio.sendResponseHeaders(200, cuerpo.length);
                    try (OutputStream salida = intercambio.getResponseBody()) {
                        salida.write(cuerpo);
                    }
                });
        servidor.start();
        return servidor;
    }

    private interface Arranque<T> {
        T arrancar() throws IOException;
    }

    private static <T> T arrancar(Arranque<T> arranque) {
        try {
            return arranque.arrancar();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
