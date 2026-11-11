package ec.mdd.generacion;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.generacion.adaptadores.salida.RepositorioDistributivosJdbc;
import ec.mdd.generacion.adaptadores.salida.TransaccionSpring;
import ec.mdd.generacion.aplicacion.ParcialesDelDistributivoServicio;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.dominio.VersionCongelada;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.infraestructura.GeneracionFlywayConfig;
import ec.mdd.generacion.puertos.entrada.VersionCongeladaEnGeneracion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
 * Congelar la versión contra PostgreSQL real (F13-H01, research §4 y §7): queda CONGELADA con el
 * hash del distributivo, es idempotente, y el bloqueo de la versión ordena la congelación y las
 * ediciones: ninguna propuesta queda después de congelar, y una lectura de la versión dentro de una
 * transacción del consolidado hace esperar a la congelación (SC-001).
 */
class CongelacionIntegrationTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-01T15:00:00Z");
    private static final String QUIEN = "coordinacion.demo";

    private static PostgreSQLContainer postgres;
    private static RepositorioDistributivosJdbc repositorio;
    private static TransaccionSpring transaccion;
    private static ParcialesDelDistributivoServicio servicio;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.6-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new GeneracionFlywayConfig().flywayGeneracion(dataSource).migrate();
        repositorio = new RepositorioDistributivosJdbc(JdbcClient.create(dataSource));
        transaccion =
                new TransaccionSpring(
                        new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
        // Congelar no usa el consolidado.
        servicio = new ParcialesDelDistributivoServicio(repositorio, transaccion, null);
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadaUnaVersionEditable_cuandoSeCongela_entoncesQuedaCongeladaConElHashDelDistributivo() {
        String periodo = enviado();
        String hashAntes = servicio.hashDelDistributivo(periodo);
        assertThat(repositorio.versionCongelada(periodo)).isEmpty();

        VersionCongeladaEnGeneracion congelada = servicio.congelarVersion(periodo);

        assertThat(congelada).isEqualTo(new VersionCongeladaEnGeneracion(1, hashAntes));
        assertThat(repositorio.vigenteDe(periodo).orElseThrow().estado())
                .isEqualTo(EstadoDeVersion.CONGELADA);
        assertThat(repositorio.versionCongelada(periodo)).hasValue(1);
        assertThat(servicio.hashDelDistributivo(periodo)).isEqualTo(hashAntes);
        // Idempotente: otra vez, lo mismo.
        assertThat(servicio.congelarVersion(periodo)).isEqualTo(congelada);
    }

    @Test
    void dadaUnaCongelacionConLaVersionBloqueada_cuandoLlegaUnaPropuesta_entoncesSeRechaza()
            throws Exception {
        String periodo = editable();
        CountDownLatch congelando = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Throwable> rechazos = Collections.synchronizedList(new ArrayList<>());
        try {
            // Como congelarVersion, pero se queda un momento con el bloqueo.
            Future<?> congelar =
                    hilos.submit(
                            () ->
                                    transaccion.ejecutar(
                                            () -> {
                                                VersionDistributivo version =
                                                        repositorio
                                                                .bloquearVigente(periodo)
                                                                .orElseThrow();
                                                repositorio.congelar(version.id());
                                                congelando.countDown();
                                                pausa();
                                                return version;
                                            }));
            congelando.await(10, TimeUnit.SECONDS);
            // Como proponer: con la versión bloqueada, exige que sea editable.
            Future<?> proponer = hilos.submit(() -> proponer(periodo, "0109990010"));
            congelar.get(20, TimeUnit.SECONDS);
            try {
                proponer.get(20, TimeUnit.SECONDS);
            } catch (ExecutionException rechazo) {
                rechazos.add(rechazo.getCause());
            }
        } finally {
            hilos.shutdownNow();
        }

        assertThat(rechazos).singleElement().isInstanceOf(VersionCongelada.class);
        assertThat(repositorio.vigenteDe(periodo).orElseThrow().propuestas()).isEmpty();
    }

    @Test
    void dadaUnaPropuestaConLaVersionBloqueada_cuandoLlegaLaCongelacion_entoncesLaEsperaYLaIncluye()
            throws Exception {
        String periodo = editable();
        String hashSinPropuesta = servicio.hashDelDistributivo(periodo);
        CountDownLatch proponiendo = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<?> proponer =
                    hilos.submit(
                            () ->
                                    transaccion.ejecutar(
                                            () -> {
                                                VersionDistributivo version =
                                                        repositorio
                                                                .bloquearVigente(periodo)
                                                                .orElseThrow();
                                                version.exigirEditable("TDS");
                                                repositorio.guardarPropuesta(
                                                        version.id(), propuesta("0109990010"));
                                                proponiendo.countDown();
                                                pausa();
                                                return version;
                                            }));
            proponiendo.await(10, TimeUnit.SECONDS);
            Future<VersionCongeladaEnGeneracion> congelar =
                    hilos.submit(() -> servicio.congelarVersion(periodo));
            proponer.get(20, TimeUnit.SECONDS);
            VersionCongeladaEnGeneracion congelada = congelar.get(20, TimeUnit.SECONDS);

            assertThat(congelada.hash()).isNotEqualTo(hashSinPropuesta);
            assertThat(congelada.hash()).isEqualTo(servicio.hashDelDistributivo(periodo));
        } finally {
            hilos.shutdownNow();
        }

        assertThat(repositorio.vigenteDe(periodo).orElseThrow().propuestas()).hasSize(1);
    }

    @Test
    void dadaUnaLecturaDeLaVersionEnUnaTransaccion_cuandoLlegaLaCongelacion_entoncesEspera()
            throws Exception {
        String periodo = enviado();
        List<String> orden = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch leida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            // Como consolidar: lee la versión dentro de su transacción, con el bloqueo compartido.
            Future<?> consolidar =
                    hilos.submit(
                            () ->
                                    transaccion.ejecutar(
                                            () -> {
                                                assertThat(repositorio.versionCongelada(periodo))
                                                        .isEmpty();
                                                leida.countDown();
                                                pausa();
                                                orden.add("consolidado");
                                                return periodo;
                                            }));
            leida.await(10, TimeUnit.SECONDS);
            Future<?> congelar =
                    hilos.submit(
                            () -> {
                                servicio.congelarVersion(periodo);
                                orden.add("congelada");
                            });
            consolidar.get(20, TimeUnit.SECONDS);
            congelar.get(20, TimeUnit.SECONDS);
        } finally {
            hilos.shutdownNow();
        }

        assertThat(orden).containsExactly("consolidado", "congelada");
    }

    private static VersionDistributivo proponer(String periodo, String docente) {
        return transaccion.ejecutar(
                () -> {
                    VersionDistributivo version =
                            repositorio.bloquearVigente(periodo).orElseThrow();
                    version.exigirEditable("TDS");
                    repositorio.guardarPropuesta(version.id(), propuesta(docente));
                    return version;
                });
    }

    private static void pausa() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }

    // Un distributivo propio con el parcial de TDS en elaboración.
    private static String editable() {
        String periodo = "P-" + UUID.randomUUID().toString().substring(0, 8);
        repositorio.crear(
                periodo,
                VersionDistributivo.primera(UUID.randomUUID(), List.of("TDS")),
                QUIEN,
                MOMENTO);
        return periodo;
    }

    // Un distributivo propio con el parcial de TDS enviado, como en EN_REVISION_VRA.
    private static String enviado() {
        String periodo = editable();
        servicio.marcarParcialEnviado(periodo, "TDS");
        return periodo;
    }

    private static Propuesta propuesta(String docente) {
        return new Propuesta(
                "POO-204",
                "A",
                "TDS",
                docente,
                "PEREZ LUIS",
                new Justificacion(TipoDeJustificacion.EXCEPCION, "Dictó la materia tres años"),
                MOMENTO,
                QUIEN);
    }
}
