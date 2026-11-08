package ec.mdd.validacionocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioConfiguracionJdbc;
import ec.mdd.validacionocs.adaptadores.salida.RepositorioProcesoJdbc;
import ec.mdd.validacionocs.adaptadores.salida.TransaccionSpring;
import ec.mdd.validacionocs.aplicacion.EtapasAlCrearse;
import ec.mdd.validacionocs.aplicacion.FlujoDelDistributivoServicio;
import ec.mdd.validacionocs.dominio.CondicionesDeRevision;
import ec.mdd.validacionocs.dominio.DevolucionDeParcial;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteDevolucion;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteRevision;
import ec.mdd.validacionocs.dominio.Firmantes;
import ec.mdd.validacionocs.dominio.ParcialesPendientes;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
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
 * La devolución contra PostgreSQL real (F12-H05, research §12): el servicio, el repositorio y la
 * transacción son los reales; Generación es un doble que lleva el estado de cada parcial. Comprueba
 * la devolución guardada con sus observaciones, una sola entrada, el reenvío que vuelve a
 * EN_CONSOLIDACION y las operaciones a la vez (SC-002, SC-004, research §11).
 */
class DevolucionIntegrationTest {

    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-11-08T15:00:00Z");
    private static final Solicitante RESPONSABLE =
            new Solicitante(
                    "consolidacion.demo", Set.of("RESPONSABLE_CONSOLIDACION"), Optional.empty());
    private static final CondicionesDeRevision LISTO =
            new CondicionesDeRevision(true, false, Optional.of("c".repeat(64)), 0, 0);
    private static final List<String> CARRERAS = List.of("DS", "TDS", "EL");

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
    void dadoEnConsolidacion_cuandoDevuelvoDosCarreras_entoncesQuedaGuardadoConSusObservaciones() {
        String periodo = enConsolidacion();

        servicio.devolver(periodo, List.of(devolucion("DS"), devolucion("TDS")), RESPONSABLE);

        assertThat(estadoGuardado(periodo)).isEqualTo("BORRADOR_CARRERA");
        assertThat(parcialesGuardados(periodo))
                .containsEntry("DS", "DEVUELTO")
                .containsEntry("TDS", "DEVUELTO")
                .containsEntry("EL", "ENVIADO");
        assertThat(devoluciones(periodo)).isEqualTo(1);
        assertThat(
                        jdbc.sql(
                                        "select o.carrera, o.texto from"
                                                + " validacionocs.observacion_de_devolucion o join"
                                                + " validacionocs.transicion t on t.id ="
                                                + " o.transicion_id where t.periodo = ? order by"
                                                + " o.carrera")
                                .param(periodo)
                                .query((fila, n) -> fila.getString(1) + ": " + fila.getString(2))
                                .list())
                .containsExactly("DS: " + observacion("DS"), "TDS: " + observacion("TDS"));
        assertThat(entradasDe(periodo, CambioDeFlujo.Accion.DISTRIBUTIVO_DEVUELTO)).hasSize(1);
        assertThat(
                        servicio.flujo(periodo, RESPONSABLE).parciales().stream()
                                .filter(uno -> uno.carrera().equals("DS"))
                                .findFirst()
                                .orElseThrow()
                                .devolucion()
                                .orElseThrow()
                                .observacion())
                .isEqualTo(observacion("DS"));
    }

    @Test
    void dadasDosDevolucionesALaVez_cuandoSeEnvian_entoncesSaleUnaSolaVez() throws Exception {
        String periodo = enConsolidacion();

        List<Throwable> rechazos =
                aLaVez(
                        () -> servicio.devolver(periodo, List.of(devolucion("DS")), RESPONSABLE),
                        () -> servicio.devolver(periodo, List.of(devolucion("DS")), RESPONSABLE));

        assertThat(devoluciones(periodo)).isEqualTo(1);
        assertThat(entradasDe(periodo, CambioDeFlujo.Accion.DISTRIBUTIVO_DEVUELTO)).hasSize(1);
        assertThat(rechazos).singleElement().isInstanceOf(EtapaNoAdmiteDevolucion.class);
    }

    @Test
    void dadasUnaDevolucionYUnEnvioARevisionALaVez_cuandoSeEnvian_entoncesPasaSoloUno()
            throws Exception {
        String periodo = enConsolidacion();

        List<Throwable> rechazos =
                aLaVez(
                        () -> servicio.devolver(periodo, List.of(devolucion("DS")), RESPONSABLE),
                        () -> servicio.enviarARevision(periodo, RESPONSABLE));

        assertThat(rechazos).hasSize(1);
        assertThat(rechazos.getFirst())
                .isInstanceOfAny(
                        EtapaNoAdmiteDevolucion.class,
                        EtapaNoAdmiteRevision.class,
                        ParcialesPendientes.class);
        assertThat(estadoGuardado(periodo)).isIn("BORRADOR_CARRERA", "EN_REVISION_VRA");
    }

    // research §11: dos devoluciones de carreras distintas a la vez. La segunda deja sus parciales
    // devueltos en Generación y enviados en Aprobación; volver a devolverlas lo arregla.
    @Test
    void
            dadasDosDevolucionesDeCarrerasDistintasALaVez_cuandoSeRepiteLaSegunda_entoncesQuedaCoherente()
                    throws Exception {
        String periodo = enConsolidacion();

        List<Throwable> rechazos =
                aLaVez(
                        () -> servicio.devolver(periodo, List.of(devolucion("DS")), RESPONSABLE),
                        () -> servicio.devolver(periodo, List.of(devolucion("TDS")), RESPONSABLE));

        assertThat(rechazos).singleElement().isInstanceOf(EtapaNoAdmiteDevolucion.class);
        String devuelta = parcialesGuardados(periodo).get("DS").equals("DEVUELTO") ? "DS" : "TDS";
        String otra = devuelta.equals("DS") ? "TDS" : "DS";
        assertThat(ENVIADOS.get(periodo).get(otra)).isFalse();
        assertThat(parcialesGuardados(periodo)).containsEntry(otra, "ENVIADO");

        // La primera carrera reenvía: vuelve a EN_CONSOLIDACION, pero el envío a revisión se
        // rechaza porque Generación no tiene la otra enviada (research §2).
        servicio.enviar(periodo, devuelta, coordinador(devuelta));
        assertThat(estadoGuardado(periodo)).isEqualTo("EN_CONSOLIDACION");
        assertThatThrownBy(() -> servicio.enviarARevision(periodo, RESPONSABLE))
                .isInstanceOf(ParcialesPendientes.class);

        // La salida: devolverla otra vez y que su carrera la reenvíe.
        servicio.devolver(periodo, List.of(devolucion(otra)), RESPONSABLE);
        servicio.enviar(periodo, otra, coordinador(otra));
        assertThat(estadoGuardado(periodo)).isEqualTo("EN_CONSOLIDACION");
        assertThat(ENVIADOS.get(periodo).values()).containsOnly(true);
        servicio.enviarARevision(periodo, RESPONSABLE);
        assertThat(estadoGuardado(periodo)).isEqualTo("EN_REVISION_VRA");
    }

    // Un periodo propio, con los parciales de sus tres carreras enviados: en EN_CONSOLIDACION.
    private static String enConsolidacion() {
        String periodo = "P-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Boolean> carreras = new LinkedHashMap<>();
        CARRERAS.forEach(carrera -> carreras.put(carrera, false));
        ENVIADOS.put(periodo, Collections.synchronizedMap(carreras));
        CARRERAS.forEach(carrera -> servicio.enviar(periodo, carrera, coordinador(carrera)));
        assertThat(estadoGuardado(periodo)).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION.name());
        return periodo;
    }

    private static Solicitante coordinador(String carrera) {
        return new Solicitante(
                "coordinacion.demo", Set.of("COORDINADOR_CARRERA"), Optional.of(carrera));
    }

    private static DevolucionDeParcial devolucion(String carrera) {
        return DevolucionDeParcial.de(carrera, observacion(carrera));
    }

    private static String observacion(String carrera) {
        return "Corrige las duras de " + carrera + ".";
    }

    private static List<Throwable> aLaVez(Runnable primera, Runnable segunda) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Throwable> rechazos = Collections.synchronizedList(new ArrayList<>());
        try {
            List<Future<?>> tareas = new ArrayList<>();
            for (Runnable una : List.of(primera, segunda)) {
                tareas.add(
                        hilos.submit(
                                () -> {
                                    esperar(salida);
                                    una.run();
                                }));
            }
            salida.countDown();
            for (Future<?> tarea : tareas) {
                try {
                    tarea.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException rechazo) {
                    rechazos.add(rechazo.getCause());
                }
            }
        } finally {
            hilos.shutdownNow();
        }
        return rechazos;
    }

    private static String estadoGuardado(String periodo) {
        return jdbc.sql("select estado from validacionocs.proceso_aprobacion where periodo = ?")
                .param(periodo)
                .query(String.class)
                .single();
    }

    private static Map<String, String> parcialesGuardados(String periodo) {
        Map<String, String> parciales = new LinkedHashMap<>();
        jdbc.sql(
                        "select carrera, estado from validacionocs.parcial_del_proceso"
                                + " where periodo = ?")
                .param(periodo)
                .query((fila, n) -> parciales.put(fila.getString(1), fila.getString(2)))
                .list();
        return parciales;
    }

    private static int devoluciones(String periodo) {
        return jdbc.sql(
                        "select count(*) from validacionocs.transicion where periodo = ?"
                                + " and tipo = 'DEVOLUCION'")
                .param(periodo)
                .query(Integer.class)
                .single();
    }

    private static List<CambioDeFlujo> entradasDe(String periodo, CambioDeFlujo.Accion accion) {
        synchronized (REGISTRADOS) {
            return REGISTRADOS.stream()
                    .filter(una -> periodo.equals(una.detalle().get("periodo")))
                    .filter(una -> una.accion() == accion)
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

    // Generación: lleva si cada parcial está enviado; devolver lo marca no enviado.
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
