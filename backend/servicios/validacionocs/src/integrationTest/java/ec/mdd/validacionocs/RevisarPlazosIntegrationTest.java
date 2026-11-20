package ec.mdd.validacionocs;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.adaptadores.salida.PublicadorDeNotificaciones;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioConfiguracionJdbc;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioPlazosJdbc;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioProcesoJdbc;
import ec.mdd.validacionocs.adaptadores.salida.TransaccionSpring;
import ec.mdd.validacionocs.aplicacion.EtapasAlCrearse;
import ec.mdd.validacionocs.aplicacion.RevisarPlazosServicio;
import ec.mdd.validacionocs.dominio.DevolucionDeParcial;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.Firmantes;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.infraestructura.ValidacionocsFlywayConfig;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * La revisión de plazos contra PostgreSQL real (F12-H03, D-114): el servicio, los repositorios, la
 * transacción y el publicador son los reales; los eventos y la bitácora se cuentan. Comprueba que
 * revisar varias veces, o dos veces a la vez, deja un vencimiento, un evento y una entrada, y que
 * otra llegada a la misma etapa vuelve a avisar (SC-002).
 */
class RevisarPlazosIntegrationTest {

    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");
    // El último parcial: el distributivo llega a EN_CONSOLIDACION, que vence el 2026-11-09.
    private static final Instant LLEGADA = Instant.parse("2026-11-04T15:00:00Z");
    private static final Instant VENCIDO = Instant.parse("2026-11-10T12:00:00Z");
    private static final String HASH = "a".repeat(64);

    private static PostgreSQLContainer postgres;
    private static JdbcClient jdbc;
    private static DataSource dataSource;
    private static RepositorioProcesoJdbc procesos;

    private final List<Object> eventos = Collections.synchronizedList(new ArrayList<>());
    private final List<CambioDeFlujo> registrados = Collections.synchronizedList(new ArrayList<>());

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.7-alpine");
        postgres.start();
        dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new ValidacionocsFlywayConfig().flywayValidacionocs(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        procesos = new RepositorioProcesoJdbc(jdbc);
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @BeforeEach
    void limpiar() {
        jdbc.sql("delete from validacionocs.proceso_aprobacion").update();
        jdbc.sql("delete from validacionocs.plazo_vencido").update();
    }

    @Test
    void dadoUnPlazoVencido_cuandoSeRevisaTresVeces_entoncesUnVencimientoUnEventoYUnaEntrada() {
        String periodo = enConsolidacion();

        assertThat(servicio(VENCIDO).revisar()).isEqualTo(1);
        assertThat(servicio(VENCIDO.plusSeconds(60)).revisar()).isZero();
        assertThat(servicio(VENCIDO.plusSeconds(86_400)).revisar()).isZero();

        assertThat(vencimientos(periodo)).isEqualTo(1);
        assertThat(eventos).hasSize(1);
        EventoEntreProcesos evento = (EventoEntreProcesos) eventos.getFirst();
        assertThat(evento.tipo()).isEqualTo("validacionocs.NotificacionesSolicitadas");
        assertThat(evento.contenido()).containsEntry("autor", "sistema");
        assertThat(registrados)
                .singleElement()
                .extracting(CambioDeFlujo::accion)
                .isEqualTo(CambioDeFlujo.Accion.PLAZO_VENCIDO);
    }

    @Test
    void dadasDosRevisionesALaVez_cuandoTerminan_entoncesQuedaUnSoloVencimientoYUnEvento()
            throws Exception {
        String periodo = enConsolidacion();
        CountDownLatch salida = new CountDownLatch(1);

        CompletableFuture<Integer> una =
                CompletableFuture.supplyAsync(
                        () -> {
                            esperar(salida);
                            return servicio(VENCIDO).revisar();
                        });
        CompletableFuture<Integer> otra =
                CompletableFuture.supplyAsync(
                        () -> {
                            esperar(salida);
                            return servicio(VENCIDO).revisar();
                        });
        salida.countDown();

        assertThat(una.get() + otra.get()).isEqualTo(1);
        assertThat(vencimientos(periodo)).isEqualTo(1);
        assertThat(eventos).hasSize(1);
    }

    @Test
    void dadaOtraLlegadaALaEtapa_cuandoVuelveAVencer_entoncesHayUnSegundoAviso() {
        String periodo = enConsolidacion();
        servicio(VENCIDO).revisar();

        // Devuelve a BORRADOR_CARRERA y vuelve a consolidación: otra llegada, otro plazo.
        ProcesoAprobacion proceso = procesos.de(periodo).orElseThrow();
        Instant devuelto = VENCIDO.plusSeconds(3600);
        List<Transicion> nuevas =
                new ArrayList<>(
                        proceso.devolver(
                                new Solicitante(
                                        "consolidacion.demo",
                                        Set.of("RESPONSABLE_CONSOLIDACION"),
                                        Optional.empty()),
                                List.of(
                                        DevolucionDeParcial.de(
                                                "DS", "Falta el docente de una asignatura.")),
                                1,
                                HASH,
                                devuelto));
        Instant reenviado = devuelto.plusSeconds(3600);
        nuevas.addAll(proceso.enviarParcial("DS", coordinador("DS"), 1, HASH, HASH, reenviado));
        procesos.guardar(proceso, nuevas);

        assertThat(servicio(reenviado.plusSeconds(6 * 86_400)).revisar()).isEqualTo(1);

        assertThat(vencimientos(periodo)).isEqualTo(2);
        assertThat(eventos).hasSize(2);
    }

    private RevisarPlazosServicio servicio(Instant ahora) {
        return new RevisarPlazosServicio(
                procesos,
                new EtapasAlCrearse(
                        new RepositorioConfiguracionJdbc(jdbc),
                        new LectorYaml(),
                        new PlantillaEnRecursos()),
                datos(),
                new RepositorioPlazosJdbc(jdbc),
                new PublicadorDeNotificaciones(eventos::add, Map::of, Clock.systemUTC()),
                registrados::add,
                new TransaccionSpring(
                        new TransactionTemplate(new DataSourceTransactionManager(dataSource))),
                Clock.fixed(ahora, ZoneOffset.UTC));
    }

    private static DatosDelPeriodo datos() {
        return new DatosDelPeriodo() {
            @Override
            public Optional<EstadoDelPeriodo> estado(String periodo) {
                return Optional.of(new EstadoDelPeriodo("EN_APROBACION"));
            }

            @Override
            public Firmantes firmantes(String periodo) {
                throw new UnsupportedOperationException();
            }
        };
    }

    // Un distributivo de dos carreras: el último parcial lo lleva a EN_CONSOLIDACION en LLEGADA.
    private static String enConsolidacion() {
        String periodo = "P-" + UUID.randomUUID().toString().substring(0, 8);
        EtapasAlCrearse etapas =
                new EtapasAlCrearse(
                        new RepositorioConfiguracionJdbc(jdbc),
                        new LectorYaml(),
                        new PlantillaEnRecursos());
        ProcesoAprobacion proceso =
                ProcesoAprobacion.nuevo(
                        periodo, etapas.de(periodo, CREADO), CREADO, List.of("DS", "TDS"));
        procesos.crear(proceso, CREADO);
        List<Transicion> nuevas =
                new ArrayList<>(
                        proceso.enviarParcial(
                                "DS", coordinador("DS"), 1, HASH, HASH, LLEGADA.minusSeconds(60)));
        nuevas.addAll(proceso.enviarParcial("TDS", coordinador("TDS"), 1, HASH, HASH, LLEGADA));
        procesos.guardar(proceso, nuevas);
        return periodo;
    }

    private static Solicitante coordinador(String carrera) {
        return new Solicitante(
                "coordinacion." + carrera, Set.of("COORDINADOR_CARRERA"), Optional.of(carrera));
    }

    private static long vencimientos(String periodo) {
        return jdbc.sql("select count(*) from validacionocs.plazo_vencido where periodo = :periodo")
                .param("periodo", periodo)
                .query(Long.class)
                .single();
    }

    private static void esperar(CountDownLatch salida) {
        try {
            salida.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
