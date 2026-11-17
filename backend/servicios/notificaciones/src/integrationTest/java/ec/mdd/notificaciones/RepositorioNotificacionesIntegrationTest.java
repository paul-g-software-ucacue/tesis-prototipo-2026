package ec.mdd.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.notificaciones.adaptadores.salida.RepositorioEventosRecibidosJdbc;
import ec.mdd.notificaciones.adaptadores.salida.RepositorioNotificacionesJdbc;
import ec.mdd.notificaciones.dominio.Notificacion;
import ec.mdd.notificaciones.infraestructura.NotificacionesFlywayConfig;
import ec.mdd.notificaciones.puertos.entrada.EventoRecibido;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Las bandejas contra PostgreSQL real (F16-H01): se insertan una sola vez por evento, destinatario
 * y carrera, se leen de la más nueva a la más antigua y se marcan como leídas una vez.
 */
class RepositorioNotificacionesIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final String ROL = "identidad_prueba";
    private static final String CLAVE = "clave-de-prueba";
    private static final Instant MOMENTO =
            Instant.parse("2026-11-15T15:00:00Z").truncatedTo(ChronoUnit.MICROS);

    private static RepositorioEventosRecibidosJdbc eventos;
    private static RepositorioNotificacionesJdbc repositorio;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        JdbcClient superusuario =
                JdbcClient.create(
                        new DriverManagerDataSource(
                                POSTGRES.getJdbcUrl(),
                                POSTGRES.getUsername(),
                                POSTGRES.getPassword()));
        superusuario.sql("create role " + ROL + " login password '" + CLAVE + "'").update();
        superusuario.sql("create schema notificaciones authorization " + ROL).update();
        DataSource proceso = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), ROL, CLAVE);
        new NotificacionesFlywayConfig().flywayNotificaciones(proceso).migrate();
        JdbcClient jdbc = JdbcClient.create(proceso);
        eventos = new RepositorioEventosRecibidosJdbc(jdbc);
        repositorio = new RepositorioNotificacionesJdbc(jdbc);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoUnEvento_cuandoSeRegistraDosVeces_entoncesSoloLaPrimeraEsNueva() {
        EventoRecibido evento = evento();

        assertThat(eventos.registrarSiEsNuevo(evento)).isTrue();
        assertThat(eventos.registrarSiEsNuevo(evento)).isFalse();
    }

    @Test
    void dadasNotificaciones_cuandoSeGuardanYSeLeen_entoncesVuelvenConSusCamposYOrdenadas() {
        EventoRecibido evento = evento();
        eventos.registrarSiEsNuevo(evento);
        Notificacion vieja =
                notificacion(evento.idEvento(), "bandeja.uno", Optional.empty(), MOMENTO);
        Notificacion nueva =
                notificacion(
                        evento.idEvento(),
                        "bandeja.uno",
                        Optional.of("DS"),
                        MOMENTO.plusSeconds(60));
        Notificacion ajena =
                notificacion(evento.idEvento(), "bandeja.otra", Optional.empty(), MOMENTO);

        repositorio.guardar(List.of(vieja, nueva, ajena));

        assertThat(repositorio.bandeja("bandeja.uno", 50)).containsExactly(nueva, vieja);
        assertThat(repositorio.bandeja("bandeja.uno", 1)).containsExactly(nueva);
        assertThat(repositorio.sinLeer("bandeja.uno")).isEqualTo(2);
        assertThat(repositorio.de(nueva.id(), "bandeja.uno")).contains(nueva);
        assertThat(repositorio.de(nueva.id(), "bandeja.otra")).isEmpty();
    }

    @Test
    void dadaLaMismaNotificacion_cuandoSeGuardaOtraVez_entoncesNoSeRepiteNiFalla() {
        EventoRecibido evento = evento();
        eventos.registrarSiEsNuevo(evento);
        Notificacion primera =
                notificacion(evento.idEvento(), "bandeja.tres", Optional.empty(), MOMENTO);
        repositorio.guardar(List.of(primera));

        repositorio.guardar(
                List.of(
                        notificacion(
                                evento.idEvento(), "bandeja.tres", Optional.empty(), MOMENTO)));

        assertThat(repositorio.bandeja("bandeja.tres", 50)).containsExactly(primera);
    }

    @Test
    void dadaUnaSinLeer_cuandoSeMarcaDosVeces_entoncesConservaLaPrimeraLectura() {
        EventoRecibido evento = evento();
        eventos.registrarSiEsNuevo(evento);
        Notificacion notificacion =
                notificacion(evento.idEvento(), "bandeja.cuatro", Optional.empty(), MOMENTO);
        repositorio.guardar(List.of(notificacion));
        Instant primera = MOMENTO.plusSeconds(120);

        repositorio.marcarLeida(notificacion.id(), primera);
        repositorio.marcarLeida(notificacion.id(), primera.plusSeconds(60));

        assertThat(repositorio.de(notificacion.id(), "bandeja.cuatro").orElseThrow().leidaEn())
                .contains(primera);
        assertThat(repositorio.sinLeer("bandeja.cuatro")).isZero();
    }

    private static EventoRecibido evento() {
        return new EventoRecibido(
                UUID.randomUUID(),
                "validacionocs.NotificacionesSolicitadas",
                "validacionocs",
                MOMENTO,
                Map.of());
    }

    private static Notificacion notificacion(
            UUID evento, String destinatario, Optional<String> carrera, Instant momento) {
        return new Notificacion(
                UUID.randomUUID(),
                evento,
                destinatario,
                carrera,
                "2026-I",
                "El distributivo llegó a «Revisión del Vicerrectorado»",
                "Periodo 2026-I, versión 1.",
                "/consolidado",
                "consolidacion.demo",
                "Responsable de consolidación",
                momento,
                MOMENTO.plusSeconds(1),
                Optional.empty());
    }
}
