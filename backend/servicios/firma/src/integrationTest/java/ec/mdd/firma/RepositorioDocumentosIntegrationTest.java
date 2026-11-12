package ec.mdd.firma;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.firma.adaptadores.salida.RepositorioDocumentosJdbc;
import ec.mdd.firma.adaptadores.salida.TransaccionSpring;
import ec.mdd.firma.dominio.DocumentoFirmable;
import ec.mdd.firma.dominio.EstadoDeGeneracion;
import ec.mdd.firma.dominio.GeneracionDeDocumento;
import ec.mdd.firma.dominio.PasoDeFirma;
import ec.mdd.firma.infraestructura.FirmaFlywayConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * Las generaciones y los documentos contra PostgreSQL real (F13-H02, research §8): guardar y leer,
 * la cadena en jsonb, y dos generaciones a la vez con un solo documento (SC-005).
 */
class RepositorioDocumentosIntegrationTest {

    private static final Instant MOMENTO =
            Instant.parse("2026-11-11T15:00:00.123456Z").truncatedTo(ChronoUnit.MICROS);
    private static final List<PasoDeFirma> CADENA =
            List.of(
                    new PasoDeFirma(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                    new PasoDeFirma(2, "Revisado", "VICERRECTOR"));

    private static PostgreSQLContainer postgres;
    private static RepositorioDocumentosJdbc repositorio;
    private static TransaccionSpring transaccion;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.6-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new FirmaFlywayConfig().flywayFirma(dataSource).migrate();
        repositorio = new RepositorioDocumentosJdbc(JdbcClient.create(dataSource));
        transaccion =
                new TransaccionSpring(
                        new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadaUnaGeneracion_cuandoSeGuardaYSeLee_entoncesQuedaIgualConSuCadena() {
        String periodo = periodo();
        GeneracionDeDocumento generacion = nueva(periodo);
        generacion.fallar("Falta el nombre del Rector.", MOMENTO);

        transaccion.ejecutar(
                () -> {
                    repositorio.guardar(generacion);
                    return null;
                });

        GeneracionDeDocumento leida =
                transaccion.ejecutar(() -> repositorio.bloquear(periodo, 1).orElseThrow());
        assertThat(leida.estado()).isEqualTo(EstadoDeGeneracion.FALLIDA);
        assertThat(leida.motivo()).contains("Falta el nombre del Rector.");
        assertThat(leida.cadena()).isEqualTo(CADENA);
        assertThat(leida.rolDeLaEtapa()).isEqualTo("RESPONSABLE_CONSOLIDACION");
        assertThat(leida.hashContenido()).isEqualTo("a".repeat(64));
        assertThat(leida.actualizadaEn()).isEqualTo(MOMENTO);
        assertThat(repositorio.ultima(periodo)).isPresent();
        assertThat(transaccion.ejecutar(() -> repositorio.bloquear(periodo, 2))).isEmpty();
    }

    @Test
    void dadoUnDocumento_cuandoSeRegistraYSeLee_entoncesQuedaIgual() {
        String periodo = periodo();
        GeneracionDeDocumento generacion = nueva(periodo);
        DocumentoFirmable documento = documento(periodo);

        transaccion.ejecutar(
                () -> {
                    repositorio.guardar(generacion);
                    repositorio.registrar(documento);
                    return null;
                });

        assertThat(repositorio.documento(periodo, 1)).contains(documento);
        assertThat(repositorio.documento(periodo, 2)).isEmpty();
    }

    @Test
    void dadasDosGeneracionesALaVez_cuandoCadaUnaRegistra_entoncesQuedaUnSoloDocumento()
            throws Exception {
        String periodo = periodo();
        transaccion.ejecutar(
                () -> {
                    repositorio.guardar(nueva(periodo));
                    return null;
                });
        CountDownLatch salida = new CountDownLatch(1);
        List<String> resultados = Collections.synchronizedList(new ArrayList<>());
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> tareas = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                tareas.add(
                        hilos.submit(
                                () -> {
                                    salida.await(10, TimeUnit.SECONDS);
                                    return transaccion.ejecutar(
                                            () -> {
                                                GeneracionDeDocumento una =
                                                        repositorio
                                                                .bloquear(periodo, 1)
                                                                .orElseThrow();
                                                if (una.estado() == EstadoDeGeneracion.GENERADA) {
                                                    resultados.add("ya estaba");
                                                    return null;
                                                }
                                                pausa();
                                                repositorio.registrar(documento(periodo));
                                                una.completar(MOMENTO);
                                                repositorio.guardar(una);
                                                resultados.add("registró");
                                                return null;
                                            });
                                }));
            }
            salida.countDown();
            for (Future<?> tarea : tareas) {
                tarea.get(30, TimeUnit.SECONDS);
            }
        } finally {
            hilos.shutdownNow();
        }

        assertThat(resultados).containsExactlyInAnyOrder("registró", "ya estaba");
        assertThat(repositorio.documento(periodo, 1)).isPresent();
    }

    private static GeneracionDeDocumento nueva(String periodo) {
        return GeneracionDeDocumento.nueva(
                periodo,
                1,
                "a".repeat(64),
                CADENA,
                "RESPONSABLE_CONSOLIDACION",
                "sistema",
                MOMENTO);
    }

    private static DocumentoFirmable documento(String periodo) {
        return new DocumentoFirmable(
                UUID.randomUUID(),
                periodo,
                1,
                "CONSOLIDADO",
                "a".repeat(64),
                "b".repeat(64),
                "documentos/" + "b".repeat(64) + ".pdf",
                2048,
                "sistema",
                MOMENTO);
    }

    private static String periodo() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static void pausa() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }
}
