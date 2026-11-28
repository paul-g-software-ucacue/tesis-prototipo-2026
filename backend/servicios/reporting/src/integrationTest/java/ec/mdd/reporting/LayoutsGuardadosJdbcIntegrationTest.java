package ec.mdd.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.reporting.adaptadores.salida.LayoutsGuardadosJdbc;
import ec.mdd.reporting.infraestructura.ReportingFlywayConfig;
import ec.mdd.reporting.puertos.salida.LayoutsGuardados.LayoutGuardado;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Las versiones del layout de exportación en PostgreSQL real (F19-H01, D-119, principio VII). */
class LayoutsGuardadosJdbcIntegrationTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-22T15:00:00Z");
    private static final String SHA = "b".repeat(64);

    private static PostgreSQLContainer postgres;
    private static JdbcClient jdbc;
    private static LayoutsGuardadosJdbc layouts;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.7-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new ReportingFlywayConfig().flywayReporting(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        layouts = new LayoutsGuardadosJdbc(jdbc);
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadasVariasCargas_cuandoSeLeen_entoncesRigeLaUltimaYSeConservanTodas() {
        assertThat(layouts.vigente()).isEmpty();
        assertThat(layouts.todos()).isEmpty();

        int primera = layouts.guardar("columnas: []", SHA, "secretaria.demo", MOMENTO);
        int segunda =
                layouts.guardar("columnas: [1]", SHA, "secretaria.demo", MOMENTO.plusSeconds(60));

        assertThat(primera).isEqualTo(1);
        assertThat(segunda).isEqualTo(2);
        assertThat(layouts.vigente())
                .contains(
                        new LayoutGuardado(
                                2,
                                "columnas: [1]",
                                SHA,
                                "secretaria.demo",
                                MOMENTO.plusSeconds(60)));
        assertThat(layouts.todos()).extracting(LayoutGuardado::numero).containsExactly(2, 1);

        assertThatThrownBy(
                        () ->
                                jdbc.sql("UPDATE reporting.layout_exportacion SET texto = 'x'")
                                        .update())
                .rootCause()
                .hasMessageContaining("no se puede modificar ni borrar");
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM reporting.layout_exportacion").update())
                .rootCause()
                .hasMessageContaining("no se puede modificar ni borrar");
        assertThat(layouts.todos()).hasSize(2);
    }

    @Test
    void elLayoutPorDefecto_esElTextoDeLaPlantilla() {
        assertThat(layouts.porDefecto()).startsWith("# Layout de exportación del CSV para SIGA");
    }
}
