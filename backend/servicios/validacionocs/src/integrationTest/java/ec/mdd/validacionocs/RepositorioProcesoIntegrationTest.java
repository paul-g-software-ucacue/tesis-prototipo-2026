package ec.mdd.validacionocs;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.validacionocs.adaptadores.salida.RepositorioProcesoJdbc;
import ec.mdd.validacionocs.dominio.EstadoDeEnvio;
import ec.mdd.validacionocs.dominio.EtapaDelProceso;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
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
        postgres = new PostgreSQLContainer("postgres:18.6-alpine");
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
                                                : "VICERRECTOR"))
                .toList();
    }

    private static String periodo() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
