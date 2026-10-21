package ec.mdd.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.academico.adaptadores.salida.RepositorioPeriodosJdbc;
import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.infraestructura.AcademicoFlywayConfig;
import java.time.Instant;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Los periodos académicos se guardan y se leen igual, campo por campo (F02-H01, T008). */
class RepositorioPeriodosJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

    private static DataSource dataSource;
    private static JdbcClient jdbc;
    private static RepositorioPeriodosJdbc repositorio;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioPeriodosJdbc(jdbc);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @BeforeEach
    void vaciar() {
        jdbc.sql("DELETE FROM academico.periodo_academico").update();
    }

    private static PeriodoAcademico periodo(String codigo, LocalDate inicio) {
        return PeriodoAcademico.nuevo(
                codigo, "Periodo " + codigo, inicio, inicio.plusMonths(5).plusDays(27));
    }

    @Test
    void dadoUnPeriodoNuevo_cuandoSeRegistra_entoncesSeLeeIgualConQuienYCuando() {
        PeriodoAcademico nuevo = periodo("2026-2", LocalDate.of(2026, 9, 1));

        assertThat(repositorio.registrar(nuevo, "vicerrectorado.demo", AHORA)).isTrue();

        assertThat(repositorio.porCodigo("2026-2")).contains(nuevo);
        assertThat(
                        jdbc.sql(
                                        "SELECT registrado_por || ' ' || (registrado_en = :ahora)"
                                                + " FROM academico.periodo_academico")
                                .param("ahora", java.sql.Timestamp.from(AHORA))
                                .query(String.class)
                                .single())
                .isEqualTo("vicerrectorado.demo true");
        assertThat(repositorio.porCodigo("2027-1")).isEmpty();
    }

    @Test
    void dadoUnCodigoQueYaExiste_cuandoSeRegistra_entoncesNoGuardaNada() {
        repositorio.registrar(periodo("2026-2", LocalDate.of(2026, 9, 1)), "a", AHORA);

        assertThat(
                        repositorio.registrar(
                                PeriodoAcademico.nuevo(
                                        "2026-2",
                                        "Otro",
                                        LocalDate.of(2030, 1, 1),
                                        LocalDate.of(2030, 6, 1)),
                                "b",
                                AHORA))
                .isFalse();
        assertThat(repositorio.porCodigo("2026-2").orElseThrow().nombre())
                .isEqualTo("Periodo 2026-2");
    }

    @Test
    void dadoVariosPeriodos_cuandoSeListan_entoncesElMasRecientePrimero() {
        repositorio.registrar(periodo("2025-2", LocalDate.of(2025, 9, 1)), "a", AHORA);
        repositorio.registrar(periodo("2026-2", LocalDate.of(2026, 9, 1)), "a", AHORA);
        repositorio.registrar(periodo("2026-1", LocalDate.of(2026, 3, 1)), "a", AHORA);

        assertThat(repositorio.todos())
                .extracting(PeriodoAcademico::codigo)
                .containsExactly("2026-2", "2026-1", "2025-2");
    }

    @Test
    void dadoUnPeriodoCorregidoYAvanzado_cuandoSeGuarda_entoncesSeLeeConSusDatosNuevos() {
        repositorio.registrar(periodo("2026-2", LocalDate.of(2026, 9, 1)), "a", AHORA);
        PeriodoAcademico cambiado =
                new PeriodoAcademico(
                        "2026-2",
                        "Periodo académico 2026-2",
                        LocalDate.of(2026, 9, 7),
                        LocalDate.of(2027, 3, 15),
                        EstadoPeriodo.EN_APROBACION);

        repositorio.guardar(cambiado);

        assertThat(repositorio.porCodigo("2026-2")).contains(cambiado);
    }

    @Test
    void dadoUnPeriodoBloqueado_cuandoOtraTransaccionLoQuiereBloquear_entoncesEspera() {
        repositorio.registrar(periodo("2026-2", LocalDate.of(2026, 9, 1)), "a", AHORA);
        TransactionTemplate transaccion =
                new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        transaccion.executeWithoutResult(
                primera -> {
                    assertThat(repositorio.bloquear("2026-2")).isPresent();
                    // Otra conexión, fuera de esta transacción, no puede tomar el mismo candado.
                    assertThatThrownBy(
                                    () ->
                                            JdbcClient.create(
                                                            new DriverManagerDataSource(
                                                                    POSTGRES.getJdbcUrl(),
                                                                    POSTGRES.getUsername(),
                                                                    POSTGRES.getPassword()))
                                                    .sql(
                                                            "SELECT codigo FROM"
                                                                    + " academico.periodo_academico"
                                                                    + " WHERE codigo = '2026-2'"
                                                                    + " FOR UPDATE NOWAIT")
                                                    .query(String.class)
                                                    .list())
                            .isInstanceOf(DataAccessException.class);
                });
        assertThat(repositorio.bloquear("2027-1")).isEmpty();
    }
}
