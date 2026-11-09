package ec.mdd.validacionocs;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.validacionocs.adaptadores.entrada.EtapaParaGeneracion;
import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioConfiguracionJdbc;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioProcesoJdbc;
import ec.mdd.validacionocs.adaptadores.salida.TransaccionSpring;
import ec.mdd.validacionocs.aplicacion.EtapaDelConsolidadoServicio;
import ec.mdd.validacionocs.aplicacion.EtapasAlCrearse;
import ec.mdd.validacionocs.aplicacion.FlujoDelDistributivoServicio;
import ec.mdd.validacionocs.dominio.CondicionesDeRevision;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.Firmantes;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.infraestructura.ValidacionocsFlywayConfig;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
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
 * El puerto que Generación requiere, contra PostgreSQL real (#215, D-99, research §2): con el
 * proceso en EN_REVISION_VRA no admite cambios, y su bloqueo compartido hace esperar a un envío a
 * revisión simultáneo, que queda con un momento posterior (SC-004).
 */
class EtapaDelConsolidadoIntegrationTest {

    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");
    private static final Solicitante RESPONSABLE =
            new Solicitante(
                    "consolidacion.demo", Set.of("RESPONSABLE_CONSOLIDACION"), Optional.empty());
    private static final CondicionesDeRevision LISTO =
            new CondicionesDeRevision(true, false, Optional.of("c".repeat(64)), 0, 0);

    private static PostgreSQLContainer postgres;
    private static JdbcClient jdbc;
    private static TransactionTemplate transacciones;
    private static FlujoDelDistributivoServicio servicio;
    private static EtapaParaGeneracion etapa;
    private static final List<CambioDeFlujo> REGISTRADOS =
            Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Map<String, Boolean>> ENVIADOS = new ConcurrentHashMap<>();

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.6-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new ValidacionocsFlywayConfig().flywayValidacionocs(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        transacciones = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        RepositorioProcesoJdbc procesos = new RepositorioProcesoJdbc(jdbc);
        etapa = new EtapaParaGeneracion(new EtapaDelConsolidadoServicio(procesos));
        servicio =
                new FlujoDelDistributivoServicio(
                        generacion(),
                        procesos,
                        new EtapasAlCrearse(
                                new RepositorioConfiguracionJdbc(jdbc),
                                new LectorYaml(),
                                new PlantillaEnRecursos()),
                        datos(),
                        REGISTRADOS::add,
                        new TransaccionSpring(transacciones),
                        Clock.systemUTC());
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadoElProcesoEnRevision_cuandoGeneracionPregunta_entoncesNoAdmiteCambios() {
        String periodo = enConsolidacion();
        assertThat(etapa.admiteCambios(periodo, CREADO)).isTrue();

        servicio.enviarARevision(periodo, RESPONSABLE);

        assertThat(etapa.admiteCambios(periodo, CREADO)).isFalse();
        assertThat(etapa.admiteCambios(periodo, Instant.parse("2026-12-01T15:00:00Z"))).isTrue();
        assertThat(etapa.admiteCambios("SIN-PROCESO", CREADO)).isTrue();
    }

    // SC-004: una operación de Generación que pregunta dentro de su transacción hace esperar al
    // envío, y la transición queda después de ella.
    @Test
    void dadaUnaTransaccionDeGeneracion_cuandoSeEnviaARevisionALaVez_entoncesElEnvioQuedaDespues()
            throws Exception {
        String periodo = enConsolidacion();
        CountDownLatch preguntado = new CountDownLatch(1);
        AtomicReference<Instant> antesDeConfirmar = new AtomicReference<>();
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<?> generacion =
                    hilos.submit(
                            () ->
                                    transacciones.executeWithoutResult(
                                            estado -> {
                                                assertThat(etapa.admiteCambios(periodo, CREADO))
                                                        .isTrue();
                                                preguntado.countDown();
                                                dormir(1500);
                                                antesDeConfirmar.set(Instant.now());
                                            }));
            preguntado.await();
            Future<?> envio = hilos.submit(() -> servicio.enviarARevision(periodo, RESPONSABLE));
            generacion.get();
            envio.get();
        } finally {
            hilos.shutdownNow();
        }

        Instant momento =
                jdbc.sql(
                                "select momento from validacionocs.transicion where periodo = ?"
                                        + " and desde = 'EN_CONSOLIDACION'")
                        .param(periodo)
                        .query(Timestamp.class)
                        .single()
                        .toInstant();
        assertThat(momento).isAfter(antesDeConfirmar.get());
    }

    private static void dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String enConsolidacion() {
        String periodo = "P-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Boolean> carreras = new LinkedHashMap<>();
        carreras.put("DS", false);
        ENVIADOS.put(periodo, Collections.synchronizedMap(carreras));
        servicio.enviar(
                periodo,
                "DS",
                new Solicitante(
                        "coordinacion.demo", Set.of("COORDINADOR_CARRERA"), Optional.of("DS")));
        return periodo;
    }

    private static Distributivo generacion() {
        return new Distributivo() {
            @Override
            public Optional<DistributivoActual> actual(String periodo) {
                return Optional.ofNullable(ENVIADOS.get(periodo))
                        .map(carreras -> new DistributivoActual(1, CREADO, Map.copyOf(carreras)));
            }

            @Override
            public ParcialEnviadoEnGeneracion marcarEnviado(String periodo, String carrera) {
                ENVIADOS.get(periodo).put(carrera, true);
                return new ParcialEnviadoEnGeneracion(1, "a".repeat(64));
            }

            @Override
            public int marcarDevueltos(String periodo, List<String> carreras) {
                carreras.forEach(carrera -> ENVIADOS.get(periodo).put(carrera, false));
                return 1;
            }

            @Override
            public String hash(String periodo) {
                return "b".repeat(64);
            }

            @Override
            public CondicionesDeRevision condicionesDeRevision(String periodo) {
                return LISTO;
            }
        };
    }

    private static DatosDelPeriodo datos() {
        return new DatosDelPeriodo() {
            @Override
            public Optional<EstadoDelPeriodo> estado(String periodo) {
                return Optional.of(new EstadoDelPeriodo("PLANIFICACION"));
            }

            @Override
            public Firmantes firmantes(String periodo) {
                return new Firmantes(Set.of(), true);
            }
        };
    }
}
