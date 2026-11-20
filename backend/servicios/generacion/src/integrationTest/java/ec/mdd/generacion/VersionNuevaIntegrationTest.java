package ec.mdd.generacion;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.generacion.adaptadores.salida.RepositorioConsolidacionJdbc;
import ec.mdd.generacion.adaptadores.salida.RepositorioDistributivosJdbc;
import ec.mdd.generacion.adaptadores.salida.TransaccionSpring;
import ec.mdd.generacion.aplicacion.ParcialesDelDistributivoServicio;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.infraestructura.GeneracionFlywayConfig;
import ec.mdd.generacion.puertos.entrada.MotivoDeVersion;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * La versión nueva contra PostgreSQL real (F12-H04, research §2, SC-002): la congelada queda
 * ANULADA sin cambiar su contenido, y la siguiente, EDITABLE, tiene todo lo de la anulada, tabla
 * por tabla. La consolidación copiada no queda desactualizada y el informe comparte su archivo.
 */
class VersionNuevaIntegrationTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-01T15:00:00Z");
    private static final Instant RECHAZO = Instant.parse("2026-11-15T16:06:00Z");
    private static final String QUIEN = "coordinacion.demo";

    /** Lo que se compara de cada tabla de la versión, sin su id ni la versión. */
    private static final Map<String, String> CONTENIDO =
            Map.of(
                    "parcial_carrera",
                    "carrera, estado",
                    "propuesta",
                    "asignatura, paralelo, carrera, docente, nombre_docente, justificacion, texto,"
                            + " propuesta_en, propuesta_por",
                    "actividad_no_lectiva",
                    "carrera, docente, nombre_docente, actividad, nombre_actividad, componente,"
                            + " proyecto, nombre_proyecto, horas, asignada_en, asignada_por",
                    "justificacion_inconsistencia",
                    "tipo, objeto, texto, justificada_por, justificada_en",
                    "consolidacion",
                    "consolidada_por, consolidada_en",
                    "informe_sustentado",
                    "nombre, tamano, huella, clave, adjuntado_por, adjuntado_en");

    private static PostgreSQLContainer postgres;
    private static JdbcClient jdbc;
    private static RepositorioDistributivosJdbc repositorio;
    private static RepositorioConsolidacionJdbc consolidacion;
    private static TransaccionSpring transaccion;
    private static ParcialesDelDistributivoServicio servicio;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.7-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new GeneracionFlywayConfig().flywayGeneracion(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioDistributivosJdbc(jdbc);
        consolidacion = new RepositorioConsolidacionJdbc(jdbc);
        transaccion =
                new TransaccionSpring(
                        new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
        servicio = new ParcialesDelDistributivoServicio(repositorio, transaccion, null);
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadaUnaVersionCongeladaConDeTodo_cuandoSeCreaLaNueva_entoncesLaCopiaEntera() {
        String periodo = congeladaConDeTodo();
        VersionDistributivo anulada = repositorio.vigenteDe(periodo).orElseThrow();
        Map<String, List<Map<String, Object>>> antes = contenido(anulada.id());
        String hashAnulada = servicio.hashDelDistributivo(periodo);

        UUID nueva =
                transaccion.ejecutar(
                        () -> {
                            VersionDistributivo bloqueada =
                                    repositorio.bloquearVigente(periodo).orElseThrow();
                            return repositorio.crearVersionNueva(
                                    bloqueada.id(),
                                    MotivoDeVersion.RECHAZO_EN_FIRMA,
                                    "sistema",
                                    RECHAZO);
                        });

        VersionDistributivo vigente = repositorio.vigenteDe(periodo).orElseThrow();
        assertThat(vigente.id()).isEqualTo(nueva);
        assertThat(vigente.numero()).isEqualTo(2);
        assertThat(vigente.estado()).isEqualTo(EstadoDeVersion.EDITABLE);
        assertThat(repositorio.estadoDe(periodo, 1)).contains(EstadoDeVersion.ANULADA);
        assertThat(repositorio.estadoDe(periodo, 2)).contains(EstadoDeVersion.EDITABLE);
        assertThat(
                        jdbc.sql(
                                        "select motivo || ' ' || creada_por || ' ' ||"
                                                + " (creada_en = :en) from"
                                                + " generacion.version_distributivo where id = :id")
                                .param("en", Timestamp.from(RECHAZO))
                                .param("id", nueva)
                                .query(String.class)
                                .single())
                .isEqualTo("RECHAZO_EN_FIRMA sistema true");
        // Tabla por tabla, lo mismo; y la anulada, intacta.
        assertThat(contenido(nueva)).isEqualTo(antes);
        assertThat(contenido(anulada.id())).isEqualTo(antes);
        antes.forEach((tabla, filas) -> assertThat(filas).as(tabla).isNotEmpty());
        // La consolidación copiada sigue al día, y el hash lleva el número de la versión.
        assertThat(consolidacion.ultimaConsolidacion(nueva))
                .isEqualTo(consolidacion.ultimaConsolidacion(anulada.id()));
        assertThat(servicio.hashDelDistributivo(periodo)).isNotEqualTo(hashAnulada);
    }

    // Un distributivo propio con la versión 1 congelada: los dos parciales enviados, una propuesta,
    // una actividad, una justificación, una consolidación y su informe.
    private static String congeladaConDeTodo() {
        String periodo = "P-" + UUID.randomUUID().toString().substring(0, 8);
        repositorio.crear(
                periodo,
                VersionDistributivo.primera(UUID.randomUUID(), List.of("TDS", "TEC")),
                QUIEN,
                MOMENTO);
        UUID version = repositorio.vigenteDe(periodo).orElseThrow().id();
        repositorio.guardarPropuesta(version, propuesta());
        jdbc.sql(
                        "insert into generacion.actividad_no_lectiva (id, version_id, carrera,"
                                + " docente, nombre_docente, actividad, nombre_actividad,"
                                + " componente, horas, asignada_en, asignada_por) values"
                                + " (:id, :version, 'TDS', '0109990010', 'PEREZ LUIS', '141a',"
                                + " 'Tutorías', 'OTRAS_DOCENCIA', 2.5, :en, :quien)")
                .param("id", UUID.randomUUID())
                .param("version", version)
                .param("en", Timestamp.from(MOMENTO))
                .param("quien", QUIEN)
                .update();
        jdbc.sql(
                        "insert into generacion.justificacion_inconsistencia (id, version_id, tipo,"
                                + " objeto, texto, justificada_por, justificada_en) values (:id,"
                                + " :version, 'SOBRECARGA', '0109990010', 'Cubre una licencia.',"
                                + " :quien, :en)")
                .param("id", UUID.randomUUID())
                .param("version", version)
                .param("quien", QUIEN)
                .param("en", Timestamp.from(MOMENTO))
                .update();
        jdbc.sql(
                        "insert into generacion.consolidacion (id, version_id, consolidada_por,"
                                + " consolidada_en) values (:id, :version, 'consolidacion.demo',"
                                + " :en)")
                .param("id", UUID.randomUUID())
                .param("version", version)
                .param("en", Timestamp.from(MOMENTO.plusSeconds(60)))
                .update();
        jdbc.sql(
                        "insert into generacion.informe_sustentado (id, version_id, nombre, tamano,"
                                + " huella, clave, adjuntado_por, adjuntado_en) values (:id,"
                                + " :version, 'informe.pdf', 1024, repeat('c', 64), :clave,"
                                + " 'consolidacion.demo', :en)")
                .param("id", UUID.randomUUID())
                .param("version", version)
                .param("clave", UUID.randomUUID().toString())
                .param("en", Timestamp.from(MOMENTO.plusSeconds(90)))
                .update();
        servicio.marcarParcialEnviado(periodo, "TDS");
        servicio.marcarParcialEnviado(periodo, "TEC");
        servicio.congelarVersion(periodo);
        return periodo;
    }

    private static Map<String, List<Map<String, Object>>> contenido(UUID version) {
        java.util.TreeMap<String, List<Map<String, Object>>> tablas = new java.util.TreeMap<>();
        CONTENIDO.forEach(
                (tabla, columnas) ->
                        tablas.put(
                                tabla,
                                jdbc.sql(
                                                "select "
                                                        + columnas
                                                        + " from generacion."
                                                        + tabla
                                                        + " where version_id = :version order by "
                                                        + columnas)
                                        .param("version", version)
                                        .query()
                                        .listOfRows()));
        return tablas;
    }

    private static Propuesta propuesta() {
        return new Propuesta(
                "POO-204",
                "A",
                "TDS",
                "0109990010",
                "PEREZ LUIS",
                new Justificacion(TipoDeJustificacion.EXCEPCION, "Dictó la materia tres años"),
                MOMENTO,
                QUIEN);
    }
}
