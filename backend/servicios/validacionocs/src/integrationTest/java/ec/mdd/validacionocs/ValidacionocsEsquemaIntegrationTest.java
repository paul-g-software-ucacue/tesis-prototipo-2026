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
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.7-alpine")) {
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
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.7-alpine")) {
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

    // F12-H04, V6: la versión nueva queda como transición, de la etapa a la misma.
    @Test
    void dadaUnaVersionNueva_cuandoSeGuardaSuTransicion_entoncesQueda() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.7-alpine")) {
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

            jdbc.sql(
                            "insert into validacionocs.transicion (id, periodo, tipo, desde, hacia,"
                                    + " version, hash, quien, momento) values (:id, '2026-I',"
                                    + " 'VERSION_NUEVA', 'EN_REVISION_VRA', 'EN_REVISION_VRA', 2,"
                                    + " :hash, 'sistema', now())")
                    .param("id", UUID.randomUUID())
                    .param("hash", "b".repeat(64))
                    .update();

            assertThat(
                            jdbc.sql(
                                            "select version from validacionocs.transicion"
                                                    + " where tipo = 'VERSION_NUEVA'")
                                    .query(Integer.class)
                                    .single())
                    .isEqualTo(2);
        }
    }

    // F18-H02, V8: la transición de una reforma, con su motivo y la versión publicada.
    @Test
    void dadaUnaReforma_cuandoSeGuardaConSuTransicion_entoncesQuedaYSuMotivoSeExige() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.7-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new ValidacionocsFlywayConfig().flywayValidacionocs(dataSource).migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);
            jdbc.sql(
                            "insert into validacionocs.proceso_aprobacion (periodo, estado, etapas,"
                                    + " distributivo_creado_en, creado_en) values ('2026-I',"
                                    + " 'EN_CONSOLIDACION', '[]'::jsonb, now(), now())")
                    .update();

            UUID transicion = transicionDeReforma(jdbc);
            reforma(jdbc, transicion, 1, "Cambia el docente de Software por una licencia.");

            assertThat(
                            jdbc.sql(
                                            "select version_publicada from validacionocs.reforma"
                                                    + " where transicion_id = :id")
                                    .param("id", transicion)
                                    .query(Integer.class)
                                    .single())
                    .isEqualTo(1);
            assertThatThrownBy(() -> reforma(jdbc, transicionDeReforma(jdbc), 1, "   corta   "))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(
                            () ->
                                    reforma(
                                            jdbc,
                                            transicionDeReforma(jdbc),
                                            0,
                                            "Un motivo de reforma con su largo."))
                    .isInstanceOf(DataAccessException.class);
        }
    }

    private static UUID transicionDeReforma(JdbcClient jdbc) {
        UUID id = UUID.randomUUID();
        jdbc.sql(
                        "insert into validacionocs.transicion (id, periodo, tipo, desde, hacia,"
                                + " version, hash, quien, momento) values (:id, '2026-I',"
                                + " 'REFORMA', 'PUBLICADO', 'EN_CONSOLIDACION', 2, :hash,"
                                + " 'vicerrectorado.demo', now())")
                .param("id", id)
                .param("hash", "c".repeat(64))
                .update();
        return id;
    }

    private static void reforma(JdbcClient jdbc, UUID transicion, int publicada, String motivo) {
        jdbc.sql(
                        "insert into validacionocs.reforma (transicion_id, periodo,"
                                + " version_publicada, motivo) values (:id, '2026-I', :publicada,"
                                + " :motivo)")
                .param("id", transicion)
                .param("publicada", publicada)
                .param("motivo", motivo)
                .update();
    }

    // F12-H03, V9: un vencimiento por llegada a la etapa, y la advertencia con la carga.
    @Test
    void dadoUnVencimiento_cuandoSeGuarda_entoncesQuedaUnoPorLlegadaYSusReglasSeExigen() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.7-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new ValidacionocsFlywayConfig().flywayValidacionocs(dataSource).migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);

            vencimiento(
                    jdbc, "EN_CONSOLIDACION", 5, "2026-11-04T15:00:00Z", "2026-11-12T12:00:00Z");

            assertThat(
                            jdbc.sql(
                                            "select fecha_limite::text from"
                                                    + " validacionocs.plazo_vencido")
                                    .query(String.class)
                                    .single())
                    .isEqualTo("2026-11-09");
            // La misma llegada no se registra dos veces.
            assertThatThrownBy(
                            () ->
                                    vencimiento(
                                            jdbc,
                                            "EN_CONSOLIDACION",
                                            5,
                                            "2026-11-04T15:00:00Z",
                                            "2026-11-13T12:00:00Z"))
                    .isInstanceOf(DataAccessException.class);
            // Otra llegada a la misma etapa, sí.
            vencimiento(
                    jdbc, "EN_CONSOLIDACION", 5, "2026-11-20T15:00:00Z", "2026-11-26T12:00:00Z");
            assertThatThrownBy(
                            () ->
                                    vencimiento(
                                            jdbc,
                                            "PUBLICADO",
                                            5,
                                            "2026-11-04T15:00:00Z",
                                            "2026-11-12T12:00:00Z"))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(
                            () ->
                                    vencimiento(
                                            jdbc,
                                            "EN_OCS",
                                            0,
                                            "2026-11-04T15:00:00Z",
                                            "2026-11-12T12:00:00Z"))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(
                            () ->
                                    vencimiento(
                                            jdbc,
                                            "EN_OCS",
                                            3,
                                            "2026-11-12T15:00:00Z",
                                            "2026-11-04T12:00:00Z"))
                    .isInstanceOf(DataAccessException.class);

            jdbc.sql(
                            "insert into validacionocs.configuracion_flujo (id, periodo, contenido,"
                                    + " cargada_por, cargada_en, advertencia) values (:id,"
                                    + " '2026-I', 'etapas: []', 'admin.demo', now(),"
                                    + " '{\"codigo\": \"RN23_SIN_NORMATIVA\"}'::jsonb)")
                    .param("id", UUID.randomUUID())
                    .update();
            assertThat(
                            jdbc.sql(
                                            "select advertencia ->> 'codigo' from"
                                                    + " validacionocs.configuracion_flujo")
                                    .query(String.class)
                                    .single())
                    .isEqualTo("RN23_SIN_NORMATIVA");
        }
    }

    private static void vencimiento(
            JdbcClient jdbc, String etapa, int dias, String llegada, String detectado) {
        jdbc.sql(
                        "insert into validacionocs.plazo_vencido (id, periodo, etapa, llegada,"
                                + " plazo_dias, fecha_limite, detectado_en) values (:id, '2026-I',"
                                + " :etapa, :llegada::timestamptz, :dias, (:llegada::timestamptz"
                                + " at time zone 'America/Guayaquil')::date + :dias,"
                                + " :detectado::timestamptz)")
                .param("id", UUID.randomUUID())
                .param("etapa", etapa)
                .param("llegada", llegada)
                .param("dias", dias)
                .param("detectado", detectado)
                .update();
    }

    // F15-H01, V7: una resolución por versión aprobada, con sus reglas.
    @Test
    void dadaUnaResolucion_cuandoSeGuardaConSuTransicion_entoncesQuedaUnaPorVersion() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.7-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new ValidacionocsFlywayConfig().flywayValidacionocs(dataSource).migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);
            jdbc.sql(
                            "insert into validacionocs.proceso_aprobacion (periodo, estado, etapas,"
                                    + " distributivo_creado_en, creado_en) values ('2026-I',"
                                    + " 'APROBADO', '[]'::jsonb, now(), now())")
                    .update();
            UUID transicion = aprobado(jdbc);

            resolucion(jdbc, transicion, 1, "RPC-SO-12-No.123-2026", "a".repeat(64), 2048);

            assertThat(
                            jdbc.sql(
                                            "select numero from validacionocs.resolucion_ocs"
                                                    + " where transicion_id = :id")
                                    .param("id", transicion)
                                    .query(String.class)
                                    .single())
                    .isEqualTo("RPC-SO-12-No.123-2026");
            assertThatThrownBy(() -> resolucion(jdbc, aprobado(jdbc), 1, "Otra", "b".repeat(64), 1))
                    .as("una por versión")
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> resolucion(jdbc, aprobado(jdbc), 2, "   ", "b".repeat(64), 1))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(
                            () ->
                                    resolucion(
                                            jdbc,
                                            aprobado(jdbc),
                                            3,
                                            "x".repeat(61),
                                            "b".repeat(64),
                                            1))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> resolucion(jdbc, aprobado(jdbc), 4, "N4", "B".repeat(64), 1))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> resolucion(jdbc, aprobado(jdbc), 5, "N5", "b".repeat(64), 0))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(
                            () ->
                                    resolucion(
                                            jdbc,
                                            aprobado(jdbc),
                                            6,
                                            "N6",
                                            "b".repeat(64),
                                            10 * 1024 * 1024 + 1))
                    .isInstanceOf(DataAccessException.class);
        }
    }

    private static UUID aprobado(JdbcClient jdbc) {
        UUID id = UUID.randomUUID();
        jdbc.sql(
                        "insert into validacionocs.transicion (id, periodo, tipo, desde, hacia,"
                                + " version, hash, quien, momento) values (:id, '2026-I',"
                                + " 'PASO_DE_ETAPA', 'EN_OCS', 'APROBADO', 1, :hash,"
                                + " 'secretaria.demo', now())")
                .param("id", id)
                .param("hash", "c".repeat(64))
                .update();
        return id;
    }

    private static void resolucion(
            JdbcClient jdbc, UUID transicion, int version, String numero, String sha, int tamano) {
        jdbc.sql(
                        "insert into validacionocs.resolucion_ocs (transicion_id, periodo, version,"
                                + " numero, fecha, sha256, nombre, tamano) values (:id, '2026-I',"
                                + " :version, :numero, current_date, :sha, 'resolucion.pdf',"
                                + " :tamano)")
                .param("id", transicion)
                .param("version", version)
                .param("numero", numero)
                .param("sha", sha)
                .param("tamano", tamano)
                .update();
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
