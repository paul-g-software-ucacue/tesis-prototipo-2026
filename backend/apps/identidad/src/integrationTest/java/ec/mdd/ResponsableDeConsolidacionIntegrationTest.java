package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.roles.puertos.entrada.AsignarRolesIniciales;
import ec.mdd.roles.puertos.entrada.DesignarResponsable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * El Responsable de consolidación en el proceso identidad completo (F05-H03, T015): Académico avisa
 * por HTTP, con su token de proceso, que un periodo se registró; el Vicerrector designa a un
 * docente, y la autorización interna le da el rol; cuando Académico avisa que el periodo se cerró,
 * ya no.
 *
 * <p>La base es un PostgreSQL de Testcontainers que se descarta al terminar.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "mdd.usuarios.procesos.datos.clave=clave-de-prueba-datos")
@Import(ErrorTecnicoIntegrationTest.Contenedores.class)
class ResponsableDeConsolidacionIntegrationTest {

    private static final Pattern TOKEN = Pattern.compile("\"token\":\"([^\"]+)\"");

    private static final int PUERTO = puertoLibre();

    private final int puerto = PUERTO;

    // El token de proceso se valida contra el JWKS de este mismo proceso, así que el puerto se
    // elige antes de arrancar.
    @DynamicPropertySource
    static void puerto(DynamicPropertyRegistry propiedades) {
        propiedades.add("server.port", () -> PUERTO);
        propiedades.add(
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:" + PUERTO + "/api/usuarios/jwks");
    }

    private static int puertoLibre() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    @Autowired private AsignarRolesIniciales rolesIniciales;

    @Autowired private DesignarResponsable designarResponsable;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void dadoUnPeriodoAvisado_cuandoSeDesignaYDespuesSeCierra_entoncesElRolEntraYSale()
            throws Exception {
        String token = tokenDeDatos();
        String periodo = "it-" + UUID.randomUUID().toString().substring(0, 8);
        rolesIniciales.asignar("docente.it", List.of("DOCENTE"));

        assertThat(avisar(token, periodo, "PLANIFICACION")).isEqualTo(204);
        designarResponsable.designar(periodo, "docente.it", "vicerrectorado.demo");

        assertThat(autorizacion(token, "docente.it"))
                .contains("RESPONSABLE_CONSOLIDACION")
                .contains("CONSOLIDACION:ESCRITURA");

        assertThat(avisar(token, periodo, "CERRADO")).isEqualTo(204);

        assertThat(autorizacion(token, "docente.it"))
                .doesNotContain("RESPONSABLE_CONSOLIDACION")
                .contains("\"DOCENTE\"");
    }

    private String tokenDeDatos() throws Exception {
        HttpResponse<String> respuesta =
                enviar(
                        "/interno/usuarios/token-proceso",
                        null,
                        "{\"proceso\":\"datos\",\"clave\":\"clave-de-prueba-datos\"}");
        assertThat(respuesta.statusCode()).isEqualTo(200);
        Matcher token = TOKEN.matcher(respuesta.body());
        assertThat(token.find()).isTrue();
        return token.group(1);
    }

    private int avisar(String token, String periodo, String estado) throws Exception {
        return enviar(
                        "/interno/roles/eventos",
                        token,
                        "{\"idEvento\":\""
                                + UUID.randomUUID()
                                + "\",\"tipo\":\"academico.PeriodoActualizado\","
                                + "\"origen\":\"academico\",\"momento\":\"2026-10-21T15:00:00Z\","
                                + "\"contenido\":{\"codigo\":\""
                                + periodo
                                + "\",\"estado\":\""
                                + estado
                                + "\"}}")
                .statusCode();
    }

    private String autorizacion(String token, String usuario) throws Exception {
        HttpResponse<String> respuesta =
                http.send(
                        HttpRequest.newBuilder(
                                        URI.create(
                                                "http://localhost:"
                                                        + puerto
                                                        + "/interno/roles/autorizaciones/"
                                                        + usuario))
                                .header("Authorization", "Bearer " + token)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(respuesta.statusCode()).isEqualTo(200);
        return respuesta.body();
    }

    private HttpResponse<String> enviar(String ruta, String token, String cuerpo) throws Exception {
        HttpRequest.Builder peticion =
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpo));
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return http.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }
}
