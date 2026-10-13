package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Escenario «Error técnico inesperado» en el proceso identidad: con la base de datos detenida, el
 * inicio de sesión responde 500 con un {@code idIncidente} y sin detalles técnicos, y el registro
 * guarda la causa con el mismo identificador (B00-H04, ADR-016).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.hikari.connection-timeout=2000")
@Import(ErrorTecnicoIntegrationTest.Contenedores.class)
@ExtendWith(OutputCaptureExtension.class)
class ErrorTecnicoIntegrationTest {

    @Autowired private PostgreSQLContainer postgres;

    @LocalServerPort private int puerto;

    @Test
    void dadoLaBaseDetenida_cuandoIniciaSesion_entonces500ConIncidenteYCausaEnElRegistro(
            CapturedOutput salida) throws Exception {
        postgres.stop();

        HttpResponse<String> respuesta =
                HttpClient.newHttpClient()
                        .send(
                                HttpRequest.newBuilder(
                                                URI.create(
                                                        "http://localhost:"
                                                                + puerto
                                                                + "/api/usuarios/sesion"))
                                        .header("Content-Type", "application/json")
                                        .POST(
                                                HttpRequest.BodyPublishers.ofString(
                                                        "{\"usuario\":\"autor.prueba\",\"clave\":\"una-clave\"}"))
                                        .build(),
                                HttpResponse.BodyHandlers.ofString());

        assertThat(respuesta.statusCode()).isEqualTo(500);
        assertThat(respuesta.body())
                .contains("\"codigo\":\"COMUN_ERROR_INTERNO\"")
                .doesNotContainIgnoringCase("sql")
                .doesNotContain("Exception")
                .doesNotContain("org.")
                .doesNotContainIgnoringCase("jdbc")
                .doesNotContain("una-clave");
        Matcher id =
                Pattern.compile("\"idIncidente\":\"([0-9a-f-]{36})\"").matcher(respuesta.body());
        assertThat(id.find()).isTrue();
        assertThat(salida.getOut())
                .contains("Incidente " + id.group(1))
                .contains("POST /api/usuarios/sesion")
                .containsIgnoringCase("connection");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Contenedores {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18.6-alpine");
        }
    }
}
