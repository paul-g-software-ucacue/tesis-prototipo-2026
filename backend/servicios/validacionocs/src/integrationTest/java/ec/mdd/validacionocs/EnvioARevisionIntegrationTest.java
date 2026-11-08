package ec.mdd.validacionocs;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioConfiguracionJdbc;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioProcesoJdbc;
import ec.mdd.validacionocs.adaptadores.salida.TransaccionSpring;
import ec.mdd.validacionocs.aplicacion.EtapasAlCrearse;
import ec.mdd.validacionocs.aplicacion.FlujoDelDistributivoServicio;
import ec.mdd.validacionocs.dominio.CondicionesDeRevision;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteRevision;
import ec.mdd.validacionocs.dominio.Firmantes;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.infraestructura.ValidacionocsFlywayConfig;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * El envío a revisión que pasa, contra PostgreSQL real (F10-H03, research §10): el servicio, el
 * repositorio y la transacción son los reales; Generación es un doble con el consolidado listo,
 * porque los datos de prueba no llegan al panel vacío. Comprueba el paso a EN_REVISION_VRA, su
 * transición guardada, una sola entrada y dos envíos a la vez con un solo paso (SC-002, SC-004).
 */
class EnvioARevisionIntegrationTest {

    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-11-08T15:00:00Z");
    private static final Solicitante RESPONSABLE =
            new Solicitante(
                    "consolidacion.demo", Set.of("RESPONSABLE_CONSOLIDACION"), Optional.empty());
    private static final CondicionesDeRevision LISTO =
            new CondicionesDeRevision(true, false, Optional.of("c".repeat(64)), 0, 0);

    private static PostgreSQLContainer postgres;
    private static JdbcClient jdbc;
    private static FlujoDelDistributivoServicio servicio;
    // Un registro falso que cuenta: la entrega real a la bitácora la prueba F17-H01.
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
        servicio =
                new FlujoDelDistributivoServicio(
                        generacion(),
                        new RepositorioProcesoJdbc(jdbc),
                        new EtapasAlCrearse(
                                new RepositorioConfiguracionJdbc(jdbc),
                                new LectorYaml(),
                                new PlantillaEnRecursos()),
                        datos(),
                        REGISTRADOS::add,
                        new TransaccionSpring(
                                new TransactionTemplate(
                                        new DataSourceTransactionManager(dataSource))),
                        Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadoUnConsolidadoListo_cuandoSeEnviaARevision_entoncesQuedaGuardadoConUnaEntrada() {
        String periodo = enConsolidacion();

        servicio.enviarARevision(periodo, RESPONSABLE);

        assertThat(estadoGuardado(periodo)).isEqualTo("EN_REVISION_VRA");
        assertThat(pasosDesdeConsolidacion(periodo)).isEqualTo(1);
        assertThat(entradasDe(periodo)).hasSize(1);
        assertThat(entradasDe(periodo).getFirst().accion())
                .isEqualTo(CambioDeFlujo.Accion.CONSOLIDADO_ENVIADO_A_REVISION);
    }

    @Test
    void dadosDosEnviosALaVez_cuandoSeEnvianARevision_entoncesPasaUnaSolaVez() throws Exception {
        String periodo = enConsolidacion();
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Throwable> rechazos = Collections.synchronizedList(new ArrayList<>());
        try {
            List<Future<?>> envios = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                envios.add(
                        hilos.submit(
                                () -> {
                                    esperar(salida);
                                    servicio.enviarARevision(periodo, RESPONSABLE);
                                }));
            }
            salida.countDown();
            for (Future<?> envio : envios) {
                try {
                    envio.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException rechazo) {
                    rechazos.add(rechazo.getCause());
                }
            }
        } finally {
            hilos.shutdownNow();
        }

        assertThat(estadoGuardado(periodo)).isEqualTo("EN_REVISION_VRA");
        assertThat(pasosDesdeConsolidacion(periodo)).isEqualTo(1);
        assertThat(entradasDe(periodo)).hasSize(1);
        assertThat(rechazos).singleElement().isInstanceOf(EtapaNoAdmiteRevision.class);
    }

    // Un periodo propio, con sus dos parciales enviados: el proceso queda en EN_CONSOLIDACION.
    private static String enConsolidacion() {
        String periodo = "P-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Boolean> carreras = new LinkedHashMap<>();
        carreras.put("DS", false);
        carreras.put("TDS", false);
        ENVIADOS.put(periodo, Collections.synchronizedMap(carreras));
        for (String carrera : List.of("DS", "TDS")) {
            servicio.enviar(
                    periodo,
                    carrera,
                    new Solicitante(
                            "coordinacion.demo",
                            Set.of("COORDINADOR_CARRERA"),
                            Optional.of(carrera)));
        }
        assertThat(estadoGuardado(periodo)).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION.name());
        return periodo;
    }

    private static String estadoGuardado(String periodo) {
        return jdbc.sql("select estado from validacionocs.proceso_aprobacion where periodo = ?")
                .param(periodo)
                .query(String.class)
                .single();
    }

    private static int pasosDesdeConsolidacion(String periodo) {
        return jdbc.sql(
                        "select count(*) from validacionocs.transicion where periodo = ? and tipo = ?"
                                + " and desde = 'EN_CONSOLIDACION'")
                .param(periodo)
                .param(TipoDeTransicion.PASO_DE_ETAPA.name())
                .query(Integer.class)
                .single();
    }

    private static List<CambioDeFlujo> entradasDe(String periodo) {
        synchronized (REGISTRADOS) {
            return REGISTRADOS.stream()
                    .filter(una -> periodo.equals(una.detalle().get("periodo")))
                    .filter(
                            una ->
                                    una.accion()
                                            == CambioDeFlujo.Accion.CONSOLIDADO_ENVIADO_A_REVISION)
                    .toList();
        }
    }

    private static void esperar(CountDownLatch salida) {
        try {
            salida.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
