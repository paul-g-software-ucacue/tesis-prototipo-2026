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
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

    // Cómo lee las reglas la versión de F04-H02 (RepositorioVersionesJdbc en 51d3c19): con JOIN.
    private static final String REGLAS_DE_LA_VERSION_ANTERIOR =
            "SELECT r.version_id, r.identificador, r.severidad, p.nombre, p.valor"
                    + " FROM normativa.regla r JOIN normativa.parametro_regla p"
                    + " ON p.version_id = r.version_id AND p.regla = r.identificador"
                    + " ORDER BY r.version_id, r.identificador, p.nombre";

    /**
     * Si la versión de F04-H03 falla en el equipo y se vuelve a la anterior (D-55), el Flyway de
     * esa versión solo conoce V1 a V3 y encuentra V4 aplicada: la tiene que ignorar como futura y
     * arrancar. Y tiene que poder leer las versiones: su consulta no ve las reglas sin parámetros,
     * así que todas las que lee siguen siendo {@code RN-xx} con al menos un parámetro, como exigía
     * su {@code Regla} (research §6 de F04-H03).
     */
    @Test
    void dadoUnEsquemaConAfinidad_cuandoArrancaLaVersionAnterior_entoncesNoFallaYLeeLasReglas(
            @TempDir Path anterior) throws IOException {
        for (String migracion :
                List.of(
                        "V1__esquema_normativa.sql",
                        "V2__politica_2024.sql",
                        "V3__versiones_y_periodos.sql")) {
            try (InputStream archivo =
                    getClass().getResourceAsStream("/db/migration/normativa/" + migracion)) {
                Files.copy(archivo, anterior.resolve(migracion));
            }
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
            List<Map<String, Object>> reglas =
                    JdbcClient.create(dataSource)
                            .sql(REGLAS_DE_LA_VERSION_ANTERIOR)
                            .query()
                            .listOfRows();
            assertThat(reglas)
                    .extracting(fila -> (String) fila.get("identificador"))
                    .allMatch(identificador -> identificador.matches("RN-[0-9]{2}"))
                    .contains("RN-19", "RN-24")
                    .doesNotContain("RN-17", "RF-24");
        }
    }

    /**
     * Una versión creada a mano antes de V4, como la 2 del equipo de demostración, queda con las
     * mismas reglas por defecto que la 1 y con su registro (D-68).
     */
    @Test
    void dadoUnaVersionCreadaAntesDeV4_cuandoSeMigra_entoncesQuedaCompletaConSuRegistro() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            Flyway completo = new NormativaFlywayConfig().flywayNormativa(dataSource);
            Flyway.configure()
                    .configuration(completo.getConfiguration())
                    .target("3")
                    .load()
                    .migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);
            jdbc.sql(
                            "insert into normativa.version_normativa (id, numero, codigo, origen,"
                                    + " vigente_desde, tipo_ist, creada_en, copiada_de)"
                                    + " values (gen_random_uuid(), 2, 'A-MANO', 'Creada a mano',"
                                    + " date '2025-01-15', 'T2', now(), 1)")
                    .update();
            jdbc.sql(
                            "insert into normativa.regla (version_id, identificador, severidad)"
                                    + " select (select id from normativa.version_normativa"
                                    + " where numero = 2), identificador, severidad"
                                    + " from normativa.regla")
                    .update();
            jdbc.sql(
                            "insert into normativa.parametro_regla (version_id, regla, nombre,"
                                    + " valor) select (select id from normativa.version_normativa"
                                    + " where numero = 2), regla, nombre, valor"
                                    + " from normativa.parametro_regla")
                    .update();

            completo.migrate();

            assertThat(reglasDe(jdbc, 2)).isEqualTo(reglasDe(jdbc, 1)).hasSize(16);
            assertThat(
                            jdbc.sql(
                                            "select (detalle ->> 'numero')::int from"
                                                    + " normativa.registro_cambio"
                                                    + " where accion = 'VERSION_EDITADA'"
                                                    + " and detalle::text not like '%actividades%'"
                                                    + " order by 1")
                                    .query(Integer.class)
                                    .list())
                    .containsExactly(1, 2);
        }
    }

    private static List<String> reglasDe(JdbcClient jdbc, int numero) {
        return jdbc.sql(
                        "select r.identificador || ':' || coalesce(r.severidad, '-')"
                                + " from normativa.regla r join normativa.version_normativa v"
                                + " on v.id = r.version_id where v.numero = :numero order by 1")
                .param("numero", numero)
                .query(String.class)
                .list();
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
