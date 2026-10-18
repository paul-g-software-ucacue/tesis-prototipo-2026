package ec.mdd.normativa;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.normativa.infraestructura.NormativaFlywayConfig;
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
class NormativaEsquemaIntegrationTest {

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

            new NormativaFlywayConfig().flywayNormativa(dataSource).migrate();

            List<String> tablas = tablasDeUsuario(dataSource);
            assertThat(tablas).contains("normativa.flyway_schema_history");
            assertThat(tablas).allMatch(tabla -> tabla.startsWith("normativa."));
        }
    }

    /**
     * Si la versión de F04-H01 falla en el equipo y se vuelve a la anterior (D-55), el Flyway de
     * esa versión solo conoce V1 y encuentra V2 aplicada: la tiene que ignorar como futura y
     * arrancar.
     */
    @Test
    void dadoUnEsquemaConLaPoliticaDe2024_cuandoArrancaLaVersionAnterior_entoncesNoFalla(
            @TempDir Path anterior) throws IOException {
        try (InputStream v1 =
                getClass()
                        .getResourceAsStream("/db/migration/normativa/V1__esquema_normativa.sql")) {
            Files.copy(v1, anterior.resolve("V1__esquema_normativa.sql"));
        }
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new NormativaFlywayConfig().flywayNormativa(dataSource).migrate();

            Flyway versionAnterior =
                    Flyway.configure()
                            .dataSource(dataSource)
                            .schemas("normativa")
                            .locations("filesystem:" + anterior)
                            .load();

            assertThat(versionAnterior.migrate().success).isTrue();
        }
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
