package ec.mdd.roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.roles.infraestructura.RolesFlywayConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
class RolesEsquemaIntegrationTest {

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

            new RolesFlywayConfig().flywayRoles(dataSource).migrate();

            List<String> tablas = tablasDeUsuario(dataSource);
            assertThat(tablas).contains("roles.flyway_schema_history");
            assertThat(tablas).allMatch(tabla -> tabla.startsWith("roles."));
        }
    }

    /**
     * Si la versión de F05-H02 falla en el equipo y se vuelve a la anterior (D-55), el Flyway de
     * esa versión solo conoce V1 y encuentra V2 y V3 aplicadas: las tiene que ignorar como futuras
     * y arrancar.
     */
    @Test
    void dadoUnEsquemaConLasMigracionesDeF05H02_cuandoArrancaLaVersionAnterior_entoncesNoFalla(
            @TempDir Path anterior) throws IOException {
        try (InputStream v1 =
                getClass().getResourceAsStream("/db/migration/roles/V1__esquema_roles.sql")) {
            Files.copy(v1, anterior.resolve("V1__esquema_roles.sql"));
        }
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new RolesFlywayConfig().flywayRoles(dataSource).migrate();

            Flyway versionAnterior =
                    Flyway.configure()
                            .dataSource(dataSource)
                            .schemas("roles")
                            .locations("filesystem:" + anterior)
                            .load();

            assertThat(versionAnterior.migrate().success).isTrue();
        }
    }

    /**
     * Las tablas del Responsable de consolidación (F05-H03, data-model.md) guardan sus reglas
     * también en la base.
     */
    @Test
    void dadoLasTablasDelResponsable_cuandoSeGuardaAlgoFueraDeSusReglas_entoncesLaBaseLoRechaza() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new RolesFlywayConfig().flywayRoles(dataSource).migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);

            assertThat(periodo(jdbc, "2026-2", "PLANIFICACION")).isEqualTo(1);
            assertThatThrownBy(() -> periodo(jdbc, "2026 3", "PLANIFICACION"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> periodo(jdbc, "2026-3", "ABIERTO"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(designacion(jdbc, "2026-2")).isEqualTo(1);
            // Una designación de un periodo que Roles no conoce viola la clave foránea.
            assertThatThrownBy(() -> designacion(jdbc, "2030-1"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(
                            jdbc.sql(
                                            "INSERT INTO roles.evento_recibido (id_evento, tipo,"
                                                    + " origen, recibido_en) VALUES"
                                                    + " (gen_random_uuid(),"
                                                    + " 'academico.PeriodoActualizado',"
                                                    + " 'academico', now())")
                                    .update())
                    .isEqualTo(1);
            assertThat(
                            jdbc.sql(
                                            "INSERT INTO roles.registro_cambio (id, momento,"
                                                    + " usuario, accion, detalle) VALUES"
                                                    + " (gen_random_uuid(), now(),"
                                                    + " 'vicerrectorado.demo',"
                                                    + " 'DESIGNAR_RESPONSABLE', '{}')")
                                    .update())
                    .isEqualTo(1);
        }
    }

    private static int periodo(JdbcClient jdbc, String codigo, String estado) {
        return jdbc.sql(
                        "INSERT INTO roles.periodo_conocido (codigo, estado, actualizado_en)"
                                + " VALUES (:codigo, :estado, now())")
                .param("codigo", codigo)
                .param("estado", estado)
                .update();
    }

    private static int designacion(JdbcClient jdbc, String periodo) {
        return jdbc.sql(
                        "INSERT INTO roles.designacion_responsable (periodo, usuario,"
                                + " designado_en, designado_por) VALUES (:periodo,"
                                + " 'docente.demo', now(), 'vicerrectorado.demo')")
                .param("periodo", periodo)
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
