package ec.mdd.roles.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.roles.dominio.EstadoConocido;
import ec.mdd.roles.dominio.PeriodoConocido;
import ec.mdd.roles.puertos.entrada.EventoRecibido;
import ec.mdd.roles.puertos.salida.RepositorioEventosRecibidos;
import ec.mdd.roles.puertos.salida.RepositorioPeriodosConocidos;
import ec.mdd.roles.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * El receptor de los avisos de Académico (F05-H03, research §1, ADR-018), con Mockito en los
 * puertos.
 */
class RecibirEventosServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

    private final RepositorioEventosRecibidos eventos = mock(RepositorioEventosRecibidos.class);
    private final RepositorioPeriodosConocidos periodos = mock(RepositorioPeriodosConocidos.class);
    private final List<String> pasos = new ArrayList<>();
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    pasos.add("abre");
                    T resultado = trabajo.get();
                    pasos.add("confirma");
                    return resultado;
                }
            };
    private final RecibirEventosServicio servicio =
            new RecibirEventosServicio(
                    eventos, periodos, transaccion, Clock.fixed(AHORA, ZoneOffset.UTC));

    private static EventoRecibido aviso(UUID id, String estado) {
        return new EventoRecibido(
                id,
                "academico.PeriodoActualizado",
                "academico",
                AHORA,
                Map.of("codigo", "2026-2", "estado", estado));
    }

    @Test
    void dadoUnAvisoNuevoDeUnPeriodoDesconocido_cuandoLlega_entoncesGuardaLaCopia() {
        UUID id = UUID.randomUUID();
        when(eventos.registrarSiEsNuevo(id, "academico.PeriodoActualizado", "academico", AHORA))
                .thenReturn(true);
        when(periodos.bloquear("2026-2")).thenReturn(Optional.empty());

        servicio.recibir(aviso(id, "PLANIFICACION"));

        verify(periodos)
                .guardar(new PeriodoConocido("2026-2", EstadoConocido.PLANIFICACION), AHORA);
        assertThat(pasos).containsExactly("abre", "confirma");
    }

    @Test
    void dadoUnAvisoNuevoDeUnPeriodoConocido_cuandoLlega_entoncesLaCopiaAvanza() {
        UUID id = UUID.randomUUID();
        when(eventos.registrarSiEsNuevo(any(), anyString(), anyString(), any())).thenReturn(true);
        when(periodos.bloquear("2026-2"))
                .thenReturn(Optional.of(new PeriodoConocido("2026-2", EstadoConocido.VIGENTE)));

        servicio.recibir(aviso(id, "CERRADO"));

        verify(periodos).guardar(new PeriodoConocido("2026-2", EstadoConocido.CERRADO), AHORA);
    }

    @Test
    void dadoUnAvisoViejoQueLlegaTarde_cuandoLlega_entoncesLaCopiaNoRetrocede() {
        when(eventos.registrarSiEsNuevo(any(), anyString(), anyString(), any())).thenReturn(true);
        when(periodos.bloquear("2026-2"))
                .thenReturn(Optional.of(new PeriodoConocido("2026-2", EstadoConocido.CERRADO)));

        servicio.recibir(aviso(UUID.randomUUID(), "PLANIFICACION"));

        verify(periodos).guardar(new PeriodoConocido("2026-2", EstadoConocido.CERRADO), AHORA);
    }

    @Test
    void dadoUnAvisoRepetido_cuandoLlegaOtraVez_entoncesNoCambiaNada() {
        when(eventos.registrarSiEsNuevo(any(), anyString(), anyString(), any())).thenReturn(false);

        servicio.recibir(aviso(UUID.randomUUID(), "VIGENTE"));

        verify(periodos, never()).guardar(any(), any());
    }

    @Test
    void dadoOtroTipoOUnContenidoInvalido_cuandoLlega_entoncesEventoNoReconocido() {
        assertThatThrownBy(
                        () ->
                                servicio.recibir(
                                        new EventoRecibido(
                                                UUID.randomUUID(),
                                                "academico.OtroEvento",
                                                "academico",
                                                AHORA,
                                                Map.of())))
                .isInstanceOf(EventoNoReconocido.class);
        assertThatThrownBy(() -> servicio.recibir(aviso(UUID.randomUUID(), "ABIERTO")))
                .isInstanceOf(EventoNoReconocido.class);
        verify(eventos, never()).registrarSiEsNuevo(any(), anyString(), anyString(), any());
    }
}
