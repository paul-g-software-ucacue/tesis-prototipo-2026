package ec.mdd.generacion;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.generacion.adaptadores.salida.RepositorioDistributivosJdbc;
import ec.mdd.generacion.adaptadores.salida.TransaccionSpring;
import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.infraestructura.GeneracionFlywayConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
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
 * Dos guardados a la vez sobre la misma versión (MD-01, D-83): el segundo espera y lee lo que
 * guardó el primero. Sin el bloqueo, los dos leerían la versión vacía.
 */
class BloqueoDeLaVersionIntegrationTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-01T15:00:00Z");
    private static final String QUIEN = "coordinacion.demo";

    private static PostgreSQLContainer postgres;
    private static RepositorioDistributivosJdbc repositorio;
    private static TransaccionSpring transaccion;

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
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadosDosGuardadosAVez_cuandoCadaUnoLee_entoncesElSegundoVeLoDelPrimero() throws Exception {
        String periodo = crear();

        guardarSiEstaVacia(periodo, "0109990010", "0109990010");

        assertThat(repositorio.vigenteDe(periodo).orElseThrow().actividades()).hasSize(1);
    }

    @Test
    void dadosDosDocentesDistintos_cuandoGuardanAVez_entoncesSeGuardanLosDos() throws Exception {
        String periodo = crear();
        CyclicBarrier juntos = new CyclicBarrier(2);
        try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
            List<Future<?>> tareas = new ArrayList<>();
            for (String docente : List.of("0109990010", "0109990020")) {
                tareas.add(
                        hilos.submit(
                                () -> {
                                    juntos.await(5, TimeUnit.SECONDS);
                                    return guardar(periodo, docente, "140-b");
                                }));
            }
            for (Future<?> tarea : tareas) {
                tarea.get(20, TimeUnit.SECONDS);
            }
        }

        assertThat(repositorio.vigenteDe(periodo).orElseThrow().actividades()).hasSize(2);
    }

    private static void guardarSiEstaVacia(String periodo, String uno, String otro)
            throws Exception {
        CyclicBarrier juntos = new CyclicBarrier(2);
        try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
            List<Future<?>> tareas = new ArrayList<>();
            int numero = 0;
            for (String docente : List.of(uno, otro)) {
                String actividad = numero == 0 ? "140-b" : "140-g";
                numero++;
                tareas.add(
                        hilos.submit(
                                () -> {
                                    juntos.await(5, TimeUnit.SECONDS);
                                    return transaccion.ejecutar(
                                            () -> {
                                                VersionDistributivo actual =
                                                        repositorio
                                                                .bloquearVigente(periodo)
                                                                .orElseThrow();
                                                pausa();
                                                if (actual.actividades().isEmpty()) {
                                                    repositorio.guardarActividad(
                                                            actual.id(),
                                                            actividad(docente, actividad));
                                                }
                                                return actual;
                                            });
                                }));
            }
            for (Future<?> tarea : tareas) {
                tarea.get(20, TimeUnit.SECONDS);
            }
        }
    }

    private static VersionDistributivo guardar(String periodo, String docente, String codigo) {
        return transaccion.ejecutar(
                () -> {
                    VersionDistributivo actual = repositorio.bloquearVigente(periodo).orElseThrow();
                    repositorio.guardarActividad(actual.id(), actividad(docente, codigo));
                    return actual;
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

    private static String crear() {
        String periodo = "P-" + UUID.randomUUID().toString().substring(0, 8);
        repositorio.crear(
                periodo,
                VersionDistributivo.primera(UUID.randomUUID(), List.of("TDS")),
                QUIEN,
                MOMENTO);
        return periodo;
    }

    private static ActividadNoLectiva actividad(String docente, String codigo) {
        return new ActividadNoLectiva(
                "TDS",
                docente,
                "PEREZ LUIS",
                codigo,
                "Planificar",
                Componente.OTRAS_DOCENCIA,
                null,
                null,
                new BigDecimal("4"),
                MOMENTO,
                QUIEN);
    }
}
