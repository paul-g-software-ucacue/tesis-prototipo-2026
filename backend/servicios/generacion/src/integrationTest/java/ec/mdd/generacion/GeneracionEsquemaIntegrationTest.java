package ec.mdd.generacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.generacion.infraestructura.GeneracionFlywayConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Migra solo este servicio contra un PostgreSQL vacío y comprueba que todas las tablas que crea
 * quedan en su esquema (escenario «Un esquema por servicio», FR-011).
 *
 * <p>Como la base solo tiene las migraciones de este servicio, cualquier tabla fuera de su esquema
 * es suya. Así se atribuye sin ambigüedad, cosa que la prueba del proceso no puede hacer porque un
 * mismo rol es dueño de los esquemas de todos sus servicios.
 */
class GeneracionEsquemaIntegrationTest {

    private static final String TABLAS_DE_USUARIO =
            "select table_schema, table_name from information_schema.tables"
                    + " where table_schema not in ('pg_catalog', 'information_schema')";

    @Test
    void dadoUnaBaseVacia_cuandoElServicioMigra_entoncesTodasSusTablasQuedanEnSuEsquema()
            throws SQLException {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

            new GeneracionFlywayConfig().flywayGeneracion(dataSource).migrate();

            List<String> tablas = tablasDeUsuario(dataSource);
            assertThat(tablas)
                    .contains("generacion.flyway_schema_history", "generacion.registro_cambio")
                    // V7 retira la tabla del evento de prueba de B00-H05 (F17-H01).
                    .doesNotContain("generacion.evento_prueba");
            assertThat(tablas).allMatch(tabla -> tabla.startsWith("generacion."));
        }
    }

    // F12-H04, V8: el motivo es nulo solo en la versión 1, y el informe comparte su archivo entre
    // versiones, pero no dentro de una.
    @Test
    void dadaLaV8_cuandoSeGuardanVersionesEInformes_entoncesElMotivoYLaClaveCumplenSusReglas() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new GeneracionFlywayConfig().flywayGeneracion(dataSource).migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);
            UUID distributivo = UUID.randomUUID();
            jdbc.sql(
                            "insert into generacion.distributivo (id, periodo, creado_en, creado_por)"
                                    + " values (:id, '2026-I', now(), 'consolidacion.demo')")
                    .param("id", distributivo)
                    .update();
            UUID v1 = version(jdbc, distributivo, 1, null);

            assertThatThrownBy(() -> version(jdbc, distributivo, 2, null))
                    .isInstanceOf(DataIntegrityViolationException.class);
            UUID otro = UUID.randomUUID();
            jdbc.sql(
                            "insert into generacion.distributivo (id, periodo, creado_en, creado_por)"
                                    + " values (:id, '2026-II', now(), 'consolidacion.demo')")
                    .param("id", otro)
                    .update();
            assertThatThrownBy(() -> version(jdbc, otro, 1, "RECHAZO_EN_FIRMA"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> version(jdbc, otro, 1, "AJUSTE"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            UUID v2 = version(jdbc, distributivo, 2, "RECHAZO_EN_FIRMA");

            informe(jdbc, v1, "clave-1");
            informe(jdbc, v2, "clave-1");
            assertThatThrownBy(() -> informe(jdbc, v2, "clave-1"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    private static UUID version(JdbcClient jdbc, UUID distributivo, int numero, String motivo) {
        UUID id = UUID.randomUUID();
        jdbc.sql(
                        "insert into generacion.version_distributivo"
                                + " (id, distributivo_id, numero, estado, motivo, creada_por,"
                                + " creada_en) values (:id, :distributivo, :numero, 'EDITABLE',"
                                + " :motivo, 'sistema', now())")
                .param("id", id)
                .param("distributivo", distributivo)
                .param("numero", numero)
                .param("motivo", motivo, java.sql.Types.VARCHAR)
                .update();
        return id;
    }

    private static void informe(JdbcClient jdbc, UUID version, String clave) {
        jdbc.sql(
                        "insert into generacion.informe_sustentado (id, version_id, nombre, tamano,"
                                + " huella, clave, adjuntado_por, adjuntado_en) values (:id,"
                                + " :version, 'informe.pdf', 10, repeat('a', 64), :clave,"
                                + " 'consolidacion.demo', now())")
                .param("id", UUID.randomUUID())
                .param("version", version)
                .param("clave", clave)
                .update();
    }

    private static List<String> tablasDeUsuario(DataSource dataSource) throws SQLException {
        List<String> tablas = new ArrayList<>();
        try (Connection conexion = dataSource.getConnection();
                Statement consulta = conexion.createStatement();
                ResultSet filas = consulta.executeQuery(TABLAS_DE_USUARIO)) {
            while (filas.next()) {
                tablas.add(filas.getString(1) + "." + filas.getString(2));
            }
        }
        return tablas;
    }
}
