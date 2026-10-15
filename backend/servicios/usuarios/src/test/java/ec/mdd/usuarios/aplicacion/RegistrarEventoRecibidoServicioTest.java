package ec.mdd.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.usuarios.puertos.entrada.EventoRecibido;
import ec.mdd.usuarios.puertos.salida.RepositorioEventosRecibidos;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** El receptor de eventos con el repositorio simulado (principio VIII: Mockito solo aquí). */
class RegistrarEventoRecibidoServicioTest {

    private final RepositorioEventosRecibidos repositorio = mock(RepositorioEventosRecibidos.class);
    private final List<EventoRecibido> aplicados = new ArrayList<>();
    private final RegistrarEventoRecibidoServicio servicio =
            new RegistrarEventoRecibidoServicio(
                    repositorio, Map.of("generacion.EventoDePrueba", aplicados::add));

    @Test
    void dadoUnEventoNuevoDeUnTipoAdmitido_cuandoSeRegistra_entoncesSeGuardaYAplicaSuManejador() {
        EventoRecibido evento = evento("generacion.EventoDePrueba");
        when(repositorio.registrarSiEsNuevo(evento)).thenReturn(true);

        servicio.registrar(evento);

        verify(repositorio).registrarSiEsNuevo(evento);
        assertThat(aplicados).containsExactly(evento);
    }

    @Test
    void dadoUnEventoYaRegistrado_cuandoLlegaOtraVez_entoncesNoVuelveAAplicarSuManejador() {
        EventoRecibido evento = evento("generacion.EventoDePrueba");
        when(repositorio.registrarSiEsNuevo(evento)).thenReturn(true, false);

        servicio.registrar(evento);
        servicio.registrar(evento);

        assertThat(aplicados).containsExactly(evento);
    }

    @Test
    void dadoUnTipoSinManejador_cuandoSeRegistra_entoncesEventoNoReconocidoSinGuardarNada() {
        assertThatThrownBy(() -> servicio.registrar(evento("generacion.OtroEvento")))
                .isInstanceOf(EventoNoReconocido.class)
                .hasMessageNotContaining("nota");
        verify(repositorio, never()).registrarSiEsNuevo(any());
        assertThat(aplicados).isEmpty();
    }

    @Test
    void dadoElReceptorDePrueba_cuandoSeArma_entoncesSoloAdmiteElEventoDePrueba() {
        assertThat(RegistrarEventoRecibidoServicio.manejadoresDePrueba())
                .containsOnlyKeys("generacion.EventoDePrueba");
    }

    private static EventoRecibido evento(String tipo) {
        return new EventoRecibido(
                UUID.randomUUID(),
                tipo,
                "generacion",
                Instant.parse("2026-10-14T15:04:05Z"),
                Map.of("nota", "evento de prueba B00-H05"));
    }
}
