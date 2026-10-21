package ec.mdd.roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.roles.adaptadores.salida.RepositorioDesignacionesJdbc;
import ec.mdd.roles.adaptadores.salida.RepositorioEventosRecibidosJdbc;
import ec.mdd.roles.adaptadores.salida.RepositorioPeriodosConocidosJdbc;
import ec.mdd.roles.dominio.Designacion;
import ec.mdd.roles.dominio.EstadoConocido;
import ec.mdd.roles.dominio.PeriodoConocido;
import ec.mdd.roles.infraestructura.RolesFlywayConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
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

/**
 * Los repositorios del Responsable de consolidación contra PostgreSQL real (F05-H03, T007): la
 * copia de los periodos, las designaciones y los eventos recibidos.
 */
class DesignacionesJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static DataSource dataSource;
    private static JdbcClient jdbc;
    private static RepositorioPeriodosConocidosJdbc periodos;
    private static RepositorioDesignacionesJdbc designaciones;
    private static RepositorioEventosRecibidosJdbc eventos;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new RolesFlywayConfig().flywayRoles(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        periodos = new RepositorioPeriodosConocidosJdbc(jdbc);
        designaciones = new RepositorioDesignacionesJdbc(jdbc);
        eventos = new RepositorioEventosRecibidosJdbc(jdbc);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @BeforeEach
    void vaciar() {
        jdbc.sql("DELETE FROM roles.designacion_responsable").update();
        jdbc.sql("DELETE FROM roles.periodo_conocido").update();
    }

    @Test
    void dadoUnPeriodoAvisado_cuandoSeGuardaYSeActualiza_entoncesSeLeeConSuEstado() {
        periodos.guardar(new PeriodoConocido("2026-2", EstadoConocido.PLANIFICACION), AHORA);
        periodos.guardar(new PeriodoConocido("2026-2", EstadoConocido.VIGENTE), AHORA);

        assertThat(periodos.buscar("2026-2"))
                .contains(new PeriodoConocido("2026-2", EstadoConocido.VIGENTE));
        assertThat(periodos.buscar("2030-1")).isEmpty();
    }

    @Test
    void dadoUnaDesignacion_cuandoSeGuardaYSeReemplaza_entoncesHayUnaPorPeriodo() {
        periodos.guardar(new PeriodoConocido("2026-2", EstadoConocido.PLANIFICACION), AHORA);
        Designacion primera =
                new Designacion("2026-2", "docente.coordinador.demo", "vicerrectorado.demo", AHORA);
        Designacion segunda =
                new Designacion("2026-2", "docente.demo", "vicerrectorado.demo", AHORA);

        designaciones.guardar(primera);
        assertThat(designaciones.dePeriodo("2026-2")).contains(primera);
        designaciones.guardar(segunda);

        assertThat(designaciones.dePeriodo("2026-2")).contains(segunda);
        assertThat(designaciones.todas()).containsExactly(segunda);
    }

    @Test
    void
            dadoDesignacionesEnPeriodosAbiertosYCerrados_cuandoSeBuscanLasDeUnUsuario_entoncesSoloLasAbiertas() {
        periodos.guardar(new PeriodoConocido("2026-1", EstadoConocido.CERRADO), AHORA);
        periodos.guardar(new PeriodoConocido("2026-2", EstadoConocido.EN_APROBACION), AHORA);
        periodos.guardar(new PeriodoConocido("2027-1", EstadoConocido.PLANIFICACION), AHORA);
        designaciones.guardar(new Designacion("2026-1", "docente.demo", "v", AHORA));
        designaciones.guardar(new Designacion("2026-2", "docente.demo", "v", AHORA));
        designaciones.guardar(new Designacion("2027-1", "otro.docente", "v", AHORA));

        assertThat(designaciones.periodosAbiertosDe("docente.demo")).containsExactly("2026-2");
        assertThat(designaciones.periodosAbiertosDe("sin.designacion")).isEmpty();
    }

    @Test
    void dadoUnPeriodoBloqueado_cuandoOtraTransaccionLoQuiereBloquear_entoncesEspera() {
        periodos.guardar(new PeriodoConocido("2026-2", EstadoConocido.PLANIFICACION), AHORA);
        TransactionTemplate transaccion =
                new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        transaccion.executeWithoutResult(
                estado -> {
                    assertThat(periodos.bloquear("2026-2")).isPresent();
                    assertThatThrownBy(
                                    () ->
                                            JdbcClient.create(
                                                            new DriverManagerDataSource(
                                                                    POSTGRES.getJdbcUrl(),
                                                                    POSTGRES.getUsername(),
                                                                    POSTGRES.getPassword()))
                                                    .sql(
                                                            "SELECT codigo FROM"
                                                                    + " roles.periodo_conocido"
                                                                    + " WHERE codigo = '2026-2'"
                                                                    + " FOR UPDATE NOWAIT")
                                                    .query(String.class)
                                                    .list())
                            .isInstanceOf(DataAccessException.class);
                });
    }

    @Test
    void dadoUnEvento_cuandoSeRegistraDosVeces_entoncesSoloLaPrimeraEsNueva() {
        UUID id = UUID.randomUUID();

        assertThat(
                        eventos.registrarSiEsNuevo(
                                id, "academico.PeriodoActualizado", "academico", AHORA))
                .isTrue();
        assertThat(
                        eventos.registrarSiEsNuevo(
                                id, "academico.PeriodoActualizado", "academico", AHORA))
                .isFalse();
    }
}
