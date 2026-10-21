package ec.mdd.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.academico.infraestructura.AcademicoFlywayConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Migra solo este servicio contra un PostgreSQL vacío y comprueba que todas las tablas que crea
 * quedan en su esquema (escenario «Un esquema por servicio», FR-011), y que las tablas de los
 * periodos guardan sus reglas también en la base (F02-H01, data-model.md).
 *
 * <p>Como la base solo tiene las migraciones de este servicio, cualquier tabla fuera de su esquema
 * es suya. Así se atribuye sin ambigüedad, cosa que la prueba del proceso no puede hacer porque un
 * mismo rol es dueño de los esquemas de todos sus servicios.
 */
class AcademicoEsquemaIntegrationTest {

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

            new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();

            List<String> tablas = tablasDeUsuario(dataSource);
            assertThat(tablas).contains("academico.flyway_schema_history");
            assertThat(tablas).allMatch(tabla -> tabla.startsWith("academico."));
            assertThat(tablas).contains("academico.periodo_academico", "academico.registro_cambio");
        }
    }

    @Test
    void dadoLasTablasDeLosPeriodos_cuandoSeGuardaAlgoFueraDeSusReglas_entoncesLaBaseLoRechaza() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);

            assertThat(periodo(jdbc, "2026-2", "Periodo 2026-2", "2027-02-28", "PLANIFICACION"))
                    .isEqualTo(1);
            for (String[] invalido :
                    new String[][] {
                        {"2026 3", "Periodo", "2027-02-28", "PLANIFICACION"},
                        {"2026-3", "  ", "2027-02-28", "PLANIFICACION"},
                        {"2026-3", "Periodo", "2026-09-01", "PLANIFICACION"},
                        {"2026-3", "Periodo", "2027-02-28", "ABIERTO"},
                        {"2026-2", "Repetido", "2027-02-28", "PLANIFICACION"}
                    }) {
                assertThatThrownBy(
                                () ->
                                        periodo(
                                                jdbc,
                                                invalido[0],
                                                invalido[1],
                                                invalido[2],
                                                invalido[3]))
                        .isInstanceOf(DataIntegrityViolationException.class);
            }
            assertThatThrownBy(
                            () ->
                                    jdbc.sql(
                                                    "INSERT INTO academico.registro_cambio"
                                                            + " (id, momento, usuario, accion,"
                                                            + " detalle) VALUES"
                                                            + " (gen_random_uuid(), now(), 'x',"
                                                            + " 'PERIODO_BORRADO', '{}')")
                                            .update())
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    private static int periodo(
            JdbcClient jdbc, String codigo, String nombre, String fin, String estado) {
        return jdbc.sql(
                        "INSERT INTO academico.periodo_academico (codigo, nombre, inicio, fin,"
                                + " estado, registrado_en, registrado_por) VALUES (:codigo,"
                                + " :nombre, DATE '2026-09-01', CAST(:fin AS date), :estado,"
                                + " now(), 'vicerrectorado.demo')")
                .param("codigo", codigo)
                .param("nombre", nombre)
                .param("fin", fin)
                .param("estado", estado)
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
