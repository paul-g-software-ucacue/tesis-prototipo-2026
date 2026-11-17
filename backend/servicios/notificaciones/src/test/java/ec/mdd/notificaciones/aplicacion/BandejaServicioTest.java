package ec.mdd.notificaciones.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.notificaciones.dominio.Notificacion;
import ec.mdd.notificaciones.dominio.NotificacionNoEncontrada;
import ec.mdd.notificaciones.puertos.salida.RepositorioNotificaciones;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** La bandeja del usuario de la sesión y marcar como leída (F16-H01, US2, FR-006 y FR-008). */
class BandejaServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-15T16:00:00Z");

    private final RepositorioNotificaciones repositorio = mock(RepositorioNotificaciones.class);
    private final BandejaServicio servicio =
            new BandejaServicio(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void dadoUnUsuario_cuandoConsultaSuBandeja_entoncesSusNotificacionesYElTotalSinLeer() {
        Notificacion una = notificacion(Optional.empty());
        when(repositorio.bandeja("vicerrectorado.demo", 50)).thenReturn(List.of(una));
        when(repositorio.sinLeer("vicerrectorado.demo")).thenReturn(7);

        assertThat(servicio.bandeja("vicerrectorado.demo", 50).notificaciones())
                .containsExactly(una);
        assertThat(servicio.bandeja("vicerrectorado.demo", 50).sinLeer()).isEqualTo(7);
        assertThat(servicio.sinLeer("vicerrectorado.demo")).isEqualTo(7);
    }

    @Test
    void dadaUnaSinLeer_cuandoSuDestinatarioLaAbre_entoncesQuedaLeidaAhora() {
        Notificacion sinLeer = notificacion(Optional.empty());
        when(repositorio.de(sinLeer.id(), "vicerrectorado.demo")).thenReturn(Optional.of(sinLeer));

        Notificacion leida = servicio.marcar(sinLeer.id(), "vicerrectorado.demo");

        assertThat(leida.leidaEn()).contains(AHORA);
        verify(repositorio).marcarLeida(sinLeer.id(), AHORA);
    }

    @Test
    void
            dadoUnRelojConNanosegundos_cuandoSeMarca_entoncesLaLecturaQuedaEnMicrosegundosComoEnLaBase() {
        Instant conNanos = Instant.parse("2026-11-15T16:00:00.123456789Z");
        BandejaServicio conReloj =
                new BandejaServicio(repositorio, Clock.fixed(conNanos, ZoneOffset.UTC));
        Notificacion sinLeer = notificacion(Optional.empty());
        when(repositorio.de(sinLeer.id(), "vicerrectorado.demo")).thenReturn(Optional.of(sinLeer));

        Notificacion leida = conReloj.marcar(sinLeer.id(), "vicerrectorado.demo");

        Instant micros = Instant.parse("2026-11-15T16:00:00.123456Z");
        assertThat(leida.leidaEn()).contains(micros);
        verify(repositorio).marcarLeida(sinLeer.id(), micros);
    }

    @Test
    void dadaUnaYaLeida_cuandoSeAbreOtraVez_entoncesNoCambiaNada() {
        Instant antes = AHORA.minusSeconds(600);
        Notificacion leida = notificacion(Optional.of(antes));
        when(repositorio.de(leida.id(), "vicerrectorado.demo")).thenReturn(Optional.of(leida));

        assertThat(servicio.marcar(leida.id(), "vicerrectorado.demo").leidaEn()).contains(antes);
        verify(repositorio, never()).marcarLeida(any(), any());
    }

    @Test
    void dadaLaDeOtroUsuarioOUnaQueNoExiste_cuandoSeMarca_entoncesNoEncontradaSinMarcar() {
        UUID id = UUID.randomUUID();
        when(repositorio.de(id, "coordinacion.demo")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.marcar(id, "coordinacion.demo"))
                .isInstanceOf(NotificacionNoEncontrada.class);
        verify(repositorio, never()).marcarLeida(any(), any());
    }

    private static Notificacion notificacion(Optional<Instant> leidaEn) {
        return new Notificacion(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "vicerrectorado.demo",
                Optional.empty(),
                "2026-I",
                "El distributivo llegó a «Revisión del Vicerrectorado»",
                "Periodo 2026-I, versión 1.",
                "/consolidado",
                "consolidacion.demo",
                "Responsable de consolidación",
                AHORA.minusSeconds(3600),
                AHORA.minusSeconds(3599),
                leidaEn);
    }
}
