package ec.mdd.validacionocs;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.validacionocs.adaptadores.salida.RepositorioProcesoJdbc;
import ec.mdd.validacionocs.dominio.EstadoDeEnvio;
import ec.mdd.validacionocs.dominio.EtapaDelProceso;
import ec.mdd.validacionocs.dominio.Observacion;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.RechazoDelFirmante;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.infraestructura.ValidacionocsFlywayConfig;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
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
 * El proceso de aprobación contra PostgreSQL real (F12-H02): se guarda con su copia de etapas, sus
 * parciales y sus transiciones en orden; un proceso de un distributivo anterior se reemplaza; y dos
 * envíos simultáneos de los dos últimos parciales pasan de etapa una sola vez (SC-004).
 */
class RepositorioProcesoIntegrationTest {

    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-11-07T15:00:00Z");
    private static final String HASH = "a".repeat(64);

    private static PostgreSQLContainer postgres;
    private static JdbcClient jdbc;
    private static RepositorioProcesoJdbc repositorio;
    private static TransactionTemplate transaccion;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.7-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new ValidacionocsFlywayConfig().flywayValidacionocs(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioProcesoJdbc(jdbc);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadoUnProcesoNuevo_cuandoSeGuarda_entoncesSeLeeConSuCopiaDeEtapasYSusParciales() {
        String periodo = periodo();
        ProcesoAprobacion nuevo = nuevo(periodo, "DS", "TDS");

        repositorio.crear(nuevo, AHORA);

        ProcesoAprobacion leido = repositorio.de(periodo).orElseThrow();
        assertThat(leido.estado()).isEqualTo(TipoDeEtapa.BORRADOR_CARRERA);
        assertThat(leido.etapas()).isEqualTo(nuevo.etapas());
        assertThat(leido.distributivoCreadoEn()).isEqualTo(CREADO);
        assertThat(leido.parciales())
                .containsEntry("DS", EstadoDeEnvio.PENDIENTE)
                .containsEntry("TDS", EstadoDeEnvio.PENDIENTE);
        assertThat(leido.transiciones()).isEmpty();
        assertThat(repositorio.de(periodo())).isEmpty();
    }

    @Test
    void dadosDosEnvios_cuandoSeGuardan_entoncesQuedanLosParcialesLaEtapaYLasTransicionesEnOrden() {
        String periodo = periodo();
        repositorio.crear(nuevo(periodo, "DS", "TDS"), AHORA);

        enviarEnUnaTransaccion(periodo, "DS", "coordinacion.demo");
        enviarEnUnaTransaccion(periodo, "TDS", "otra.coordinacion");

        ProcesoAprobacion leido = repositorio.de(periodo).orElseThrow();
        assertThat(leido.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(leido.parciales()).containsEntry("TDS", EstadoDeEnvio.ENVIADO);
        assertThat(leido.transiciones())
                .extracting(Transicion::tipo, Transicion::carrera, Transicion::quien)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                TipoDeTransicion.ENVIO_PARCIAL,
                                Optional.of("DS"),
                                "coordinacion.demo"),
                        org.assertj.core.groups.Tuple.tuple(
                                TipoDeTransicion.ENVIO_PARCIAL,
                                Optional.of("TDS"),
                                "otra.coordinacion"),
                        org.assertj.core.groups.Tuple.tuple(
                                TipoDeTransicion.PASO_DE_ETAPA,
                                Optional.empty(),
                                "otra.coordinacion"));
        assertThat(leido.transiciones().get(2).hacia()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
    }

    @Test
    void dadaUnaCarreraNueva_cuandoSeGuardaElProceso_entoncesSuParcialSeAgrega() {
        String periodo = periodo();
        repositorio.crear(nuevo(periodo, "DS"), AHORA);
        ProcesoAprobacion proceso = repositorio.de(periodo).orElseThrow();
        proceso.sumarParciales(List.of("TDS"));

        repositorio.guardar(proceso, List.of());

        assertThat(repositorio.de(periodo).orElseThrow().parciales())
                .containsEntry("TDS", EstadoDeEnvio.PENDIENTE);
    }

    @Test
    void dadoElProcesoDeUnDistributivoAnterior_cuandoSeCreaOtro_entoncesLoReemplazaConTodo() {
        String periodo = periodo();
        repositorio.crear(nuevo(periodo, "DS"), AHORA);
        enviarEnUnaTransaccion(periodo, "DS", "coordinacion.demo");
        Instant otroDistributivo = CREADO.plusSeconds(86_400);

        repositorio.crear(
                ProcesoAprobacion.nuevo(periodo, etapas(), otroDistributivo, List.of("AGRO")),
                AHORA);

        ProcesoAprobacion leido = repositorio.de(periodo).orElseThrow();
        assertThat(leido.distributivoCreadoEn()).isEqualTo(otroDistributivo);
        assertThat(leido.parciales()).containsOnlyKeys("AGRO");
        assertThat(leido.transiciones()).isEmpty();
        assertThat(leido.estado()).isEqualTo(TipoDeEtapa.BORRADOR_CARRERA);
    }

    @Test
    void dadosDosEnviosALaVezDeLosDosUltimos_cuandoSeGuardan_entoncesPasaDeEtapaUnaSolaVez()
            throws Exception {
        String periodo = periodo();
        repositorio.crear(nuevo(periodo, "DS", "TDS"), AHORA);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<?> uno =
                    hilos.submit(
                            () -> {
                                esperar(salida);
                                enviarEnUnaTransaccion(periodo, "DS", "coordinacion.demo");
                            });
            Future<?> otro =
                    hilos.submit(
                            () -> {
                                esperar(salida);
                                enviarEnUnaTransaccion(periodo, "TDS", "otra.coordinacion");
                            });
            salida.countDown();
            uno.get(30, TimeUnit.SECONDS);
            otro.get(30, TimeUnit.SECONDS);
        } finally {
            hilos.shutdownNow();
        }

        ProcesoAprobacion leido = repositorio.de(periodo).orElseThrow();
        assertThat(leido.estado()).isEqualTo(TipoDeEtapa.EN_CONSOLIDACION);
        assertThat(leido.transiciones())
                .filteredOn(una -> una.tipo() == TipoDeTransicion.PASO_DE_ETAPA)
                .hasSize(1);
        assertThat(leido.transiciones()).hasSize(3);
    }

    // Como lo hará el caso de uso: bloquear, cambiar el agregado y guardar, en una transacción.
    private static void enviarEnUnaTransaccion(String periodo, String carrera, String quien) {
        transaccion.executeWithoutResult(
                estado -> {
                    ProcesoAprobacion proceso = repositorio.bloquear(periodo).orElseThrow();
                    List<Transicion> nuevas =
                            proceso.enviarParcial(
                                    carrera,
                                    new Solicitante(
                                            quien, Set.of("COORDINADOR_CARRERA"), Optional.empty()),
                                    1,
                                    HASH,
                                    "b".repeat(64),
                                    AHORA);
                    repositorio.guardar(proceso, nuevas);
                });
    }

    @Test
    void dadoUnRechazoEnFirma_cuandoSeGuarda_entoncesSeLeeConSuRechazoYQuedaPendiente() {
        String periodo = periodo();
        ProcesoAprobacion enRevision =
                ProcesoAprobacion.de(
                        periodo,
                        etapas(),
                        TipoDeEtapa.EN_REVISION_VRA,
                        CREADO,
                        java.util.Map.of("DS", EstadoDeEnvio.ENVIADO),
                        List.of());
        repositorio.crear(enRevision, AHORA);
        ProcesoAprobacion proceso = repositorio.de(periodo).orElseThrow();
        repositorio.guardar(
                proceso,
                proceso.aprobarContenido(
                        new Solicitante(
                                "vicerrectorado.demo", Set.of("VICERRECTOR"), Optional.empty()),
                        1,
                        HASH,
                        AHORA));
        RechazoDelFirmante rechazo =
                new RechazoDelFirmante(
                        3,
                        "RECTOR",
                        "rectorado.demo",
                        AHORA.plusSeconds(60),
                        new Observacion("Falta la carga de un docente de Software."));
        ProcesoAprobacion enFirma = repositorio.de(periodo).orElseThrow();

        repositorio.guardar(enFirma, enFirma.rechazarEnFirma(1, rechazo, AHORA.plusSeconds(61)));

        ProcesoAprobacion leido = repositorio.de(periodo).orElseThrow();
        assertThat(leido.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        assertThat(leido.transiciones().getLast().tipo())
                .isEqualTo(TipoDeTransicion.RECHAZO_EN_FIRMA);
        assertThat(leido.transiciones().getLast().quien()).isEqualTo("sistema");
        assertThat(leido.transiciones().getLast().rechazo()).contains(rechazo);
        assertThat(leido.rechazoPendiente()).isPresent();
    }

    // F12-H04: el rechazo y la versión nueva se guardan juntos y se leen en orden.
    @Test
    void dadoUnRechazoConSuVersionNueva_cuandoSeGuardan_entoncesSeLeenYYaNoHayRechazoPendiente() {
        String periodo = periodo();
        repositorio.crear(
                ProcesoAprobacion.de(
                        periodo,
                        etapas(),
                        TipoDeEtapa.EN_REVISION_VRA,
                        CREADO,
                        java.util.Map.of("DS", EstadoDeEnvio.ENVIADO),
                        List.of()),
                AHORA);
        ProcesoAprobacion proceso = repositorio.de(periodo).orElseThrow();
        repositorio.guardar(
                proceso,
                proceso.aprobarContenido(
                        new Solicitante(
                                "vicerrectorado.demo", Set.of("VICERRECTOR"), Optional.empty()),
                        1,
                        HASH,
                        AHORA));
        RechazoDelFirmante rechazo =
                new RechazoDelFirmante(
                        2,
                        "VICERRECTOR",
                        "vicerrectorado.demo",
                        AHORA.plusSeconds(60),
                        new Observacion("Falta la carga de un docente de Software."));
        ProcesoAprobacion enFirma = repositorio.de(periodo).orElseThrow();
        List<ec.mdd.validacionocs.dominio.Transicion> nuevas =
                new java.util.ArrayList<>(
                        enFirma.rechazarEnFirma(1, rechazo, AHORA.plusSeconds(61)));
        nuevas.addAll(enFirma.registrarVersionNueva(1, 2, "e".repeat(64), AHORA.plusSeconds(61)));

        repositorio.guardar(enFirma, nuevas);

        ProcesoAprobacion leido = repositorio.de(periodo).orElseThrow();
        assertThat(leido.estado()).isEqualTo(TipoDeEtapa.EN_REVISION_VRA);
        assertThat(leido.transiciones())
                .extracting(ec.mdd.validacionocs.dominio.Transicion::tipo)
                .containsExactly(
                        TipoDeTransicion.PASO_DE_ETAPA,
                        TipoDeTransicion.RECHAZO_EN_FIRMA,
                        TipoDeTransicion.VERSION_NUEVA);
        assertThat(leido.transiciones().getLast().version()).isEqualTo(2);
        assertThat(leido.transiciones().getLast().quien()).isEqualTo("sistema");
        assertThat(leido.rechazoPendiente()).isEmpty();
        assertThat(leido.rechazoVigente()).contains(leido.transiciones().get(1));
    }

    // F15-H01: el paso a APROBADO con su resolución se guarda y se lee.
    @Test
    void dadaLaResolucionDelOcs_cuandoSeGuarda_entoncesSeLeeConSuTransicion() {
        String periodo = periodo();
        repositorio.crear(
                ProcesoAprobacion.de(
                        periodo,
                        etapas(),
                        TipoDeEtapa.EN_REVISION_VRA,
                        CREADO,
                        java.util.Map.of("DS", EstadoDeEnvio.ENVIADO),
                        List.of()),
                AHORA);
        ProcesoAprobacion proceso = repositorio.de(periodo).orElseThrow();
        repositorio.guardar(
                proceso,
                proceso.aprobarContenido(
                        new Solicitante(
                                "vicerrectorado.demo", Set.of("VICERRECTOR"), Optional.empty()),
                        1,
                        HASH,
                        AHORA));
        ProcesoAprobacion enFirma = repositorio.de(periodo).orElseThrow();
        repositorio.guardar(enFirma, enFirma.completarFirmas(1, AHORA.plusSeconds(60)));
        ProcesoAprobacion enOcs = repositorio.de(periodo).orElseThrow();
        ec.mdd.validacionocs.dominio.ResolucionOcs resolucion =
                new ec.mdd.validacionocs.dominio.ResolucionOcs(
                        "RPC-SO-12-No.123-2026",
                        java.time.LocalDate.ofInstant(AHORA, ProcesoAprobacion.ZONA),
                        "b".repeat(64),
                        "resolucion.pdf",
                        2048);

        repositorio.guardar(
                enOcs,
                // En las etapas de esta prueba, EN_OCS es del VICERRECTOR.
                enOcs.registrarResolucion(
                        new Solicitante("secretaria.demo", Set.of("VICERRECTOR"), Optional.empty()),
                        resolucion,
                        AHORA.plusSeconds(120)));

        ProcesoAprobacion leido = repositorio.de(periodo).orElseThrow();
        assertThat(leido.estado()).isEqualTo(TipoDeEtapa.APROBADO);
        assertThat(leido.resolucion())
                .get()
                .satisfies(
                        una -> {
                            assertThat(una.quien()).isEqualTo("secretaria.demo");
                            assertThat(una.resolucion()).contains(resolucion);
                        });
    }

    // F15-H01, FR-010: dos registros a la vez; el segundo espera el bloqueo y encuentra APROBADO.
    @Test
    void dadosDosRegistrosDeLaResolucionALaVez_cuandoSeGuardan_entoncesQuedaUnaSola()
            throws Exception {
        String periodo = enOcs();
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Throwable> rechazos =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        try {
            List<Future<?>> tareas = new java.util.ArrayList<>();
            for (String numero : List.of("RPC-SO-12-No.123-2026", "RPC-SO-12-No.124-2026")) {
                tareas.add(
                        hilos.submit(
                                () -> {
                                    esperar(salida);
                                    try {
                                        registrarEnUnaTransaccion(periodo, numero);
                                    } catch (RuntimeException rechazo) {
                                        rechazos.add(rechazo);
                                    }
                                }));
            }
            salida.countDown();
            for (Future<?> tarea : tareas) {
                tarea.get(30, TimeUnit.SECONDS);
            }
        } finally {
            hilos.shutdownNow();
        }

        assertThat(rechazos)
                .singleElement()
                .isInstanceOf(ec.mdd.validacionocs.dominio.EtapaNoAdmiteResolucion.class);
        ProcesoAprobacion leido = repositorio.de(periodo).orElseThrow();
        assertThat(leido.estado()).isEqualTo(TipoDeEtapa.APROBADO);
        assertThat(leido.transiciones()).filteredOn(una -> una.resolucion().isPresent()).hasSize(1);
    }

    // F18-H01, FR-004: dos publicaciones a la vez; la segunda espera el bloqueo y encuentra
    // PUBLICADO.
    @Test
    void dadasDosPublicacionesALaVez_cuandoSeGuardan_entoncesQuedaUnaSola() throws Exception {
        String periodo = enOcs();
        registrarEnUnaTransaccion(periodo, "RPC-SO-12-No.123-2026");
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Throwable> rechazos =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        try {
            List<Future<?>> tareas = new java.util.ArrayList<>();
            for (int i = 0; i < 2; i++) {
                tareas.add(
                        hilos.submit(
                                () -> {
                                    esperar(salida);
                                    try {
                                        publicarEnUnaTransaccion(periodo);
                                    } catch (RuntimeException rechazo) {
                                        rechazos.add(rechazo);
                                    }
                                }));
            }
            salida.countDown();
            for (Future<?> tarea : tareas) {
                tarea.get(30, TimeUnit.SECONDS);
            }
        } finally {
            hilos.shutdownNow();
        }

        assertThat(rechazos)
                .singleElement()
                .isInstanceOf(ec.mdd.validacionocs.dominio.EtapaNoAdmitePublicacion.class);
        ProcesoAprobacion leido = repositorio.de(periodo).orElseThrow();
        assertThat(leido.estado()).isEqualTo(TipoDeEtapa.PUBLICADO);
        assertThat(leido.transiciones())
                .filteredOn(una -> una.hacia() == TipoDeEtapa.PUBLICADO)
                .hasSize(1);
        assertThat(leido.publicacion()).isPresent();
    }

    // Como PublicarDistributivoServicio: el proceso bloqueado, el agregado decide y se guarda.
    private static void publicarEnUnaTransaccion(String periodo) {
        transaccion.executeWithoutResult(
                estado -> {
                    ProcesoAprobacion bloqueado = repositorio.bloquear(periodo).orElseThrow();
                    pausa();
                    repositorio.guardar(
                            bloqueado,
                            bloqueado.publicar(
                                    // En las etapas de esta prueba, APROBADO es del VICERRECTOR.
                                    new Solicitante(
                                            "secretaria.demo",
                                            Set.of("VICERRECTOR"),
                                            Optional.empty()),
                                    AHORA.plusSeconds(180)));
                });
    }

    private String enOcs() {
        String periodo = periodo();
        repositorio.crear(
                ProcesoAprobacion.de(
                        periodo,
                        etapas(),
                        TipoDeEtapa.EN_REVISION_VRA,
                        CREADO,
                        java.util.Map.of("DS", EstadoDeEnvio.ENVIADO),
                        List.of()),
                AHORA);
        ProcesoAprobacion proceso = repositorio.de(periodo).orElseThrow();
        repositorio.guardar(
                proceso,
                proceso.aprobarContenido(
                        new Solicitante(
                                "vicerrectorado.demo", Set.of("VICERRECTOR"), Optional.empty()),
                        1,
                        HASH,
                        AHORA));
        ProcesoAprobacion enFirma = repositorio.de(periodo).orElseThrow();
        repositorio.guardar(enFirma, enFirma.completarFirmas(1, AHORA.plusSeconds(60)));
        return periodo;
    }

    // Como RegistrarResolucionServicio: el proceso bloqueado, el agregado decide y se guarda.
    private static void registrarEnUnaTransaccion(String periodo, String numero) {
        transaccion.executeWithoutResult(
                estado -> {
                    ProcesoAprobacion bloqueado = repositorio.bloquear(periodo).orElseThrow();
                    pausa();
                    repositorio.guardar(
                            bloqueado,
                            bloqueado.registrarResolucion(
                                    // En las etapas de esta prueba, EN_OCS es del VICERRECTOR.
                                    new Solicitante(
                                            "secretaria.demo",
                                            Set.of("VICERRECTOR"),
                                            Optional.empty()),
                                    new ec.mdd.validacionocs.dominio.ResolucionOcs(
                                            numero,
                                            java.time.LocalDate.ofInstant(
                                                    AHORA, ProcesoAprobacion.ZONA),
                                            "b".repeat(64),
                                            "resolucion.pdf",
                                            2048),
                                    AHORA.plusSeconds(120)));
                });
    }

    private static void pausa() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }

    private static void esperar(CountDownLatch salida) {
        try {
            salida.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static ProcesoAprobacion nuevo(String periodo, String... carreras) {
        return ProcesoAprobacion.nuevo(periodo, etapas(), CREADO, List.of(carreras));
    }

    private static List<EtapaDelProceso> etapas() {
        return Arrays.stream(TipoDeEtapa.values())
                .map(
                        tipo ->
                                new EtapaDelProceso(
                                        tipo,
                                        tipo != TipoDeEtapa.BORRADOR_UNIDADES
                                                && tipo != TipoDeEtapa.REMITIDO_ZONAL,
                                        "Nombre de " + tipo,
                                        tipo == TipoDeEtapa.BORRADOR_CARRERA
                                                ? "COORDINADOR_CARRERA"
                                                : "VICERRECTOR",
                                        // Distinto por etapa, para ver que se guarda (F12-H05).
                                        tipo == TipoDeEtapa.EN_CONSOLIDACION))
                .toList();
    }

    private static String periodo() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
