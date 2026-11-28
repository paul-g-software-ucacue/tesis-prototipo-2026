package ec.mdd.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.reporting.adaptadores.salida.ExportacionesSigaJdbc;
import ec.mdd.reporting.dominio.ExportacionSiga;
import ec.mdd.reporting.dominio.SumaDeVerificacion;
import ec.mdd.reporting.infraestructura.ReportingFlywayConfig;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Las exportaciones a SIGA en PostgreSQL real (F19-H02, D-120, principio VII): se guardan, se leen
 * por periodo de la más reciente a la más antigua, y no se modifican ni se borran.
 */
class ExportacionesSigaJdbcIntegrationTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-28T15:00:00Z");
    private static final String HASH = "a".repeat(64);
    private static final SumaDeVerificacion SUMA =
            SumaDeVerificacion.de("ID\r\n".getBytes(StandardCharsets.UTF_8));

    private static PostgreSQLContainer postgres;
    private static JdbcClient jdbc;
    private static ExportacionesSigaJdbc exportaciones;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.7-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new ReportingFlywayConfig().flywayReporting(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        exportaciones = new ExportacionesSigaJdbc(jdbc);
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    private static ExportacionSiga exportacion(String periodo, Instant momento, int layout) {
        return new ExportacionSiga(
                UUID.randomUUID(),
                "secretaria.demo",
                momento,
                periodo,
                1,
                HASH,
                layout,
                "siga-" + periodo + "-v1.csv",
                165,
                SUMA);
    }

    @Test
    void dadasVariasExportaciones_cuandoSeLeenPorPeriodo_entoncesLaMasRecientePrimeroYSoloDeEse() {
        ExportacionSiga primera = exportacion("2026-A", MOMENTO, 0);
        ExportacionSiga segunda = exportacion("2026-A", MOMENTO.plusSeconds(60), 2);
        ExportacionSiga otra = exportacion("2026-B", MOMENTO, 0);

        exportaciones.guardar(primera);
        exportaciones.guardar(segunda);
        exportaciones.guardar(otra);

        assertThat(exportaciones.delPeriodo("2026-A")).containsExactly(segunda, primera);
        assertThat(exportaciones.delPeriodo("2026-B")).containsExactly(otra);
        assertThat(exportaciones.delPeriodo("2026-C")).isEmpty();
    }

    @Test
    void dadaUnaExportacionGuardada_cuandoSeModificaOSeBorra_entoncesLaBaseLoRechaza() {
        exportaciones.guardar(exportacion("2026-D", MOMENTO, 0));

        assertThatThrownBy(
                        () -> jdbc.sql("UPDATE reporting.exportacion_siga SET filas = 0").update())
                .rootCause()
                .hasMessageContaining("no se puede modificar ni borrar");
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM reporting.exportacion_siga").update())
                .rootCause()
                .hasMessageContaining("no se puede modificar ni borrar");
        assertThat(exportaciones.delPeriodo("2026-D"))
                .singleElement()
                .extracting(ExportacionSiga::filas)
                .isEqualTo(165);
    }

    @Test
    void dadasDosExportacionesALaVez_cuandoSeGuardan_entoncesQuedanLasDos() {
        CountDownLatch salida = new CountDownLatch(1);
        List<CompletableFuture<Void>> guardados =
                List.of(0, 1).stream()
                        .map(
                                layout ->
                                        CompletableFuture.runAsync(
                                                () -> {
                                                    try {
                                                        salida.await();
                                                    } catch (InterruptedException interrumpido) {
                                                        Thread.currentThread().interrupt();
                                                    }
                                                    exportaciones.guardar(
                                                            exportacion("2026-E", MOMENTO, layout));
                                                }))
                        .toList();

        salida.countDown();
        guardados.forEach(CompletableFuture::join);

        assertThat(exportaciones.delPeriodo("2026-E")).hasSize(2);
    }
}
