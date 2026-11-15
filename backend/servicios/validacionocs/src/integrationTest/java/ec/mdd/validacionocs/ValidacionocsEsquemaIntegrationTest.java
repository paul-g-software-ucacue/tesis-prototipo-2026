package ec.mdd.validacionocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.validacionocs.infraestructura.ValidacionocsFlywayConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
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
class ValidacionocsEsquemaIntegrationTest {

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

            new ValidacionocsFlywayConfig().flywayValidacionocs(dataSource).migrate();

            List<String> tablas = tablasDeUsuario(dataSource);
            assertThat(tablas).contains("validacionocs.flyway_schema_history");
            assertThat(tablas).allMatch(tabla -> tabla.startsWith("validacionocs."));
            // El proceso de aprobación de F12-H02 (data-model.md).
            assertThat(tablas)
                    .contains(
                            "validacionocs.proceso_aprobacion",
                            "validacionocs.parcial_del_proceso",
                            "validacionocs.transicion");
        }
    }

    @Test
    void dadoUnRechazoEnFirma_cuandoSeGuardaConSuTransicion_entoncesQuedaYSuObservacionSeExige() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new ValidacionocsFlywayConfig().flywayValidacionocs(dataSource).migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);
            jdbc.sql(
                            "insert into validacionocs.proceso_aprobacion (periodo, estado, etapas,"
                                    + " distributivo_creado_en, creado_en) values ('2026-I',"
                                    + " 'EN_REVISION_VRA', '[]'::jsonb, now(), now())")
                    .update();
            UUID transicion = UUID.randomUUID();
            jdbc.sql(
                            "insert into validacionocs.transicion (id, periodo, tipo, desde, hacia,"
                                    + " version, hash, quien, momento) values (:id, '2026-I',"
                                    + " 'RECHAZO_EN_FIRMA', 'CONGELADO_EN_FIRMA', 'EN_REVISION_VRA',"
                                    + " 1, :hash, 'sistema', now())")
                    .param("id", transicion)
                    .param("hash", "a".repeat(64))
                    .update();

            rechazo(jdbc, transicion, "Falta la carga de un docente de Software.");

            assertThat(
                            jdbc.sql(
                                            "select observacion from validacionocs.rechazo_en_firma"
                                                    + " where transicion_id = :id")
                                    .param("id", transicion)
                                    .query(String.class)
                                    .single())
                    .isEqualTo("Falta la carga de un docente de Software.");
            assertThatThrownBy(() -> rechazo(jdbc, transicion, "Otra observación más."))
                    .as("un rechazo por transición")
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> rechazo(jdbc, UUID.randomUUID(), "   corta   "))
                    .isInstanceOf(DataAccessException.class);
        }
    }

    private static void rechazo(JdbcClient jdbc, UUID transicion, String observacion) {
        jdbc.sql(
                        "insert into validacionocs.rechazo_en_firma (transicion_id, orden, rol,"
                                + " rechazado_por, rechazado_en, observacion) values (:id, 2,"
                                + " 'VICERRECTOR', 'vicerrectorado.demo', now(), :observacion)")
                .param("id", transicion)
                .param("observacion", observacion)
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
