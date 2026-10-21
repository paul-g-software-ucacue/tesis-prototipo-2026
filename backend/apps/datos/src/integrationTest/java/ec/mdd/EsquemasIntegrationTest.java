package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Levanta el proceso completo contra un PostgreSQL real, de modo que sus servicios migran juntos, y
 * comprueba que cada servicio deja su registro de migraciones en su esquema y que no hay tablas
 * fuera de los esquemas del proceso (escenario «Un esquema por servicio», FR-011).
 *
 * <p>Los esquemas esperados salen de los beans {@link Flyway} del contexto, así que la prueba sirve
 * igual para cualquier proceso. No puede atribuir una tabla a un servicio dentro del proceso; eso
 * lo hace la prueba de integración de cada servicio.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(EsquemasIntegrationTest.Contenedores.class)
class EsquemasIntegrationTest {

    @Autowired private List<Flyway> flyways;

    @Autowired private JdbcTemplate jdbc;

    @Test
    void
            dadoElProcesoArrancado_cuandoSusServiciosMigran_entoncesNingunaTablaQuedaFueraDeSusEsquemas() {
        Set<String> esperados =
                flyways.stream()
                        .flatMap(flyway -> Arrays.stream(flyway.getConfiguration().getSchemas()))
                        .collect(Collectors.toSet());
        List<String> tablas =
                jdbc.query(
                        "select table_schema || '.' || table_name from information_schema.tables"
                                + " where table_schema not in ('pg_catalog', 'information_schema')",
                        (fila, numero) -> fila.getString(1));

        assertThat(tablas)
                .containsAll(
                        esperados.stream()
                                .map(esquema -> esquema + ".flyway_schema_history")
                                .toList());
        // El registro de publicaciones, que migra flywayEventos: Académico avisa a Roles (F05-H03,
        // ADR-018).
        assertThat(tablas).contains("eventos_datos.event_publication");
        assertThat(tablas).noneMatch(tabla -> tabla.startsWith("public."));
        assertThat(tablas)
                .allMatch(tabla -> esperados.contains(tabla.substring(0, tabla.indexOf('.'))));
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
