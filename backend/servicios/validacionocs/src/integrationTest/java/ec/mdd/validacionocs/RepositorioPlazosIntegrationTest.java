package ec.mdd.validacionocs;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.validacionocs.adaptadores.salida.RepositorioPlazosJdbc;
import ec.mdd.validacionocs.dominio.Plazo;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.VencimientoDePlazo;
import ec.mdd.validacionocs.infraestructura.ValidacionocsFlywayConfig;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Los vencimientos de plazo contra PostgreSQL real (F12-H03, D-114): uno por llegada a la etapa, y
 * registrar otra vez la misma llegada no inserta ni falla.
 */
class RepositorioPlazosIntegrationTest {

    private static final Instant LLEGADA = Instant.parse("2026-11-04T15:00:00Z");

    private static PostgreSQLContainer postgres;
    private static JdbcClient jdbc;
    private static RepositorioPlazosJdbc repositorio;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.7-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new ValidacionocsFlywayConfig().flywayValidacionocs(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioPlazosJdbc(jdbc);
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadoUnVencimiento_cuandoSeRegistra_entoncesQuedaConSuFechaLimite() {
        String periodo = periodo();

        assertThat(repositorio.registrar(vencimiento(periodo, LLEGADA, 8))).isTrue();

        assertThat(
                        jdbc.sql(
                                        "select fecha_limite from validacionocs.plazo_vencido"
                                                + " where periodo = :periodo")
                                .param("periodo", periodo)
                                .query(LocalDate.class)
                                .single())
                .isEqualTo(LocalDate.of(2026, 11, 9));
    }

    @Test
    void dadaLaMismaLlegada_cuandoSeRegistraOtraVez_entoncesNoInsertaNiFalla() {
        String periodo = periodo();
        repositorio.registrar(vencimiento(periodo, LLEGADA, 8));

        assertThat(repositorio.registrar(vencimiento(periodo, LLEGADA, 9))).isFalse();
        assertThat(filas(periodo)).isEqualTo(1);
    }

    @Test
    void dadaOtraLlegadaALaMismaEtapa_cuandoSeRegistra_entoncesQuedanLasDos() {
        String periodo = periodo();
        repositorio.registrar(vencimiento(periodo, LLEGADA, 8));

        assertThat(
                        repositorio.registrar(
                                vencimiento(periodo, LLEGADA.plus(Duration.ofDays(10)), 18)))
                .isTrue();
        assertThat(filas(periodo)).isEqualTo(2);
    }

    private static VencimientoDePlazo vencimiento(String periodo, Instant llegada, int dias) {
        return new VencimientoDePlazo(
                periodo,
                Plazo.de(TipoDeEtapa.EN_CONSOLIDACION, llegada, 5),
                llegada.plus(Duration.ofDays(dias)),
                dias - 5);
    }

    private static long filas(String periodo) {
        return jdbc.sql("select count(*) from validacionocs.plazo_vencido where periodo = :periodo")
                .param("periodo", periodo)
                .query(Long.class)
                .single();
    }

    private static String periodo() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
