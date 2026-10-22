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
 * periodos (F02-H01) y de la estructura orgánica (F03-H01) guardan sus reglas también en la base
 * (data-model.md).
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
            assertThat(tablas)
                    .contains(
                            "academico.periodo_academico",
                            "academico.registro_cambio",
                            "academico.gestion_autorizada",
                            "academico.unidad_organica");
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

    @Test
    void dadaLaMigracion_cuandoSeLeeElCatalogo_entoncesTraeLas21GestionesDePol6() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            JdbcClient jdbc = migrada(postgres);

            List<String> codigos =
                    jdbc.sql("SELECT codigo FROM academico.gestion_autorizada ORDER BY codigo")
                            .query(String.class)
                            .list();
            assertThat(codigos).hasSize(21).startsWith("G01").endsWith("G21");
            assertThat(nombre(jdbc, "G01")).isEqualTo("Rector");
            assertThat(nombre(jdbc, "G07"))
                    .isEqualTo("Gestor de Investigación, Desarrollo Tecnológico e Innovación");
            assertThat(nombre(jdbc, "G21")).isEqualTo("Gestor Unidad de Estadísticas");
        }
    }

    @Test
    void dadaLaTablaDeLasUnidades_cuandoSeGuardaAlgoFueraDeSusReglas_entoncesLaBaseLoRechaza() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            JdbcClient jdbc = migrada(postgres);

            assertThat(unidad(jdbc, "MATRIZ", "SEDE_MATRIZ", null, null, null)).isEqualTo(1);
            assertThat(unidad(jdbc, "RECT", "GOBIERNO", "MATRIZ", "G01", null)).isEqualTo(1);
            assertThat(unidad(jdbc, "G-INV", "GESTION", "RECT", "G07", null)).isEqualTo(1);
            assertThat(unidad(jdbc, "COM-X", "COMISION", "RECT", null, true)).isEqualTo(1);
            for (Object[] invalida :
                    new Object[][] {
                        {"MATRIZ2", "SEDE_MATRIZ", null, null, null},
                        {"SEDE-M", "SEDE_MATRIZ", "MATRIZ", null, null},
                        {"OTRA", "SEDE", null, null, null},
                        {"G-SIN", "GESTION", "RECT", null, null},
                        {"G-X", "GESTION", "RECT", "G99", null},
                        {"COM-G", "COMISION", "RECT", "G07", true},
                        {"COM-S", "COMISION", "RECT", null, null},
                        {"G-P", "GESTION", "RECT", "G07", true},
                        {"HUERFANA", "GOBIERNO", "NADA", null, null},
                        {"g-min", "GOBIERNO", "RECT", null, null},
                        {"TIPO", "OFICINA", "RECT", null, null},
                        {"G-INV", "GOBIERNO", "RECT", null, null}
                    }) {
                assertThatThrownBy(
                                () ->
                                        unidad(
                                                jdbc,
                                                (String) invalida[0],
                                                (String) invalida[1],
                                                (String) invalida[2],
                                                (String) invalida[3],
                                                (Boolean) invalida[4]))
                        .as("unidad %s", invalida[0])
                        .isInstanceOf(DataIntegrityViolationException.class);
            }
            for (String accion :
                    new String[] {
                        "UNIDAD_REGISTRADA",
                        "UNIDAD_CORREGIDA",
                        "UNIDAD_INACTIVADA",
                        "UNIDAD_REACTIVADA",
                        "PERIODO_REGISTRADO"
                    }) {
                assertThat(
                                jdbc.sql(
                                                "INSERT INTO academico.registro_cambio (id, momento,"
                                                        + " usuario, accion, detalle) VALUES"
                                                        + " (gen_random_uuid(), now(), 'x', :accion,"
                                                        + " '{}')")
                                        .param("accion", accion)
                                        .update())
                        .isEqualTo(1);
            }
        }
    }

    private static JdbcClient migrada(PostgreSQLContainer postgres) {
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();
        return JdbcClient.create(dataSource);
    }

    private static String nombre(JdbcClient jdbc, String codigo) {
        return jdbc.sql("SELECT nombre FROM academico.gestion_autorizada WHERE codigo = :codigo")
                .param("codigo", codigo)
                .query(String.class)
                .single();
    }

    private static int unidad(
            JdbcClient jdbc,
            String codigo,
            String tipo,
            String padre,
            String gestion,
            Boolean permanente) {
        return jdbc.sql(
                        "INSERT INTO academico.unidad_organica (codigo, nombre, tipo, padre,"
                                + " gestion_autorizada, comision_permanente, activa, registrada_en,"
                                + " registrada_por) VALUES (:codigo, 'Unidad', :tipo, :padre,"
                                + " :gestion, :permanente, true, now(), 'admin.demo')")
                .param("codigo", codigo)
                .param("tipo", tipo)
                .param("padre", padre)
                .param("gestion", gestion)
                .param("permanente", permanente)
                .update();
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
