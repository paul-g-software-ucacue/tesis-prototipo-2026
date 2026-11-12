package ec.mdd.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.usuarios.dominio.EntradaBitacora;
import ec.mdd.usuarios.dominio.EntradaBitacoraInvalida;
import ec.mdd.usuarios.puertos.entrada.EventoRecibido;
import ec.mdd.usuarios.puertos.salida.RepositorioBitacora;
import ec.mdd.usuarios.puertos.salida.RepositorioEventosRecibidos;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/**
 * El receptor de la bitácora con los repositorios simulados (principio VIII: Mockito solo aquí).
 */
class RegistrarEventoRecibidoServicioTest {

    private final RepositorioEventosRecibidos eventos = mock(RepositorioEventosRecibidos.class);
    private final RepositorioBitacora bitacora = mock(RepositorioBitacora.class);
    private final RegistrarEventoRecibidoServicio servicio =
            new RegistrarEventoRecibidoServicio(eventos, bitacora);

    @ParameterizedTest
    @ValueSource(
            strings = {
                "generacion",
                "academico",
                "docentes",
                "normativa",
                "roles",
                "validacionocs",
                "firma"
            })
    void dadoUnaEntradaNuevaDeCadaServicio_cuandoSeRegistra_entoncesQuedaEnLaBitacora(
            String servicioDeOrigen) {
        EventoRecibido evento = evento(servicioDeOrigen + ".EntradaBitacora", servicioDeOrigen);
        when(eventos.registrarSiEsNuevo(evento)).thenReturn(true);

        servicio.registrar(evento);

        ArgumentCaptor<EntradaBitacora> registrada = ArgumentCaptor.forClass(EntradaBitacora.class);
        verify(bitacora).registrar(registrada.capture());
        assertThat(registrada.getValue().idEvento()).isEqualTo(evento.idEvento());
        assertThat(registrada.getValue().servicio()).isEqualTo(servicioDeOrigen);
        assertThat(registrada.getValue().anterior()).contains(Map.of("horas", "4"));
        assertThat(registrada.getValue().nuevo()).contains(Map.of("horas", "6"));
    }

    @Test
    void dadoUnaEntradaYaRegistrada_cuandoLlegaOtraVez_entoncesNoSeRegistraDosVeces() {
        EventoRecibido evento = evento("generacion.EntradaBitacora", "generacion");
        when(eventos.registrarSiEsNuevo(evento)).thenReturn(true, false);

        servicio.registrar(evento);
        servicio.registrar(evento);

        verify(eventos, times(2)).registrarSiEsNuevo(evento);
        verify(bitacora, times(1)).registrar(any());
    }

    @Test
    void dadoUnTipoQueNoEsDeLaBitacora_cuandoSeRegistra_entoncesEventoNoReconocidoSinGuardarNada() {
        assertThatThrownBy(
                        () -> servicio.registrar(evento("generacion.EventoDePrueba", "generacion")))
                .isInstanceOf(EventoNoReconocido.class)
                .hasMessageNotContaining("TUTORIAS");
        verify(eventos, never()).registrarSiEsNuevo(any());
        verify(bitacora, never()).registrar(any());
    }

    @Test
    void dadoUnServicioQueNoRegistraEnLaBitacora_cuandoSeRegistra_entoncesEventoNoReconocido() {
        assertThatThrownBy(() -> servicio.registrar(evento("cargas.EntradaBitacora", "cargas")))
                .isInstanceOf(EventoNoReconocido.class);
        verify(eventos, never()).registrarSiEsNuevo(any());
    }

    @Test
    void dadoUnPrefijoDistintoDelOrigen_cuandoSeRegistra_entoncesEventoNoReconocido() {
        assertThatThrownBy(() -> servicio.registrar(evento("roles.EntradaBitacora", "generacion")))
                .isInstanceOf(EventoNoReconocido.class);
        verify(eventos, never()).registrarSiEsNuevo(any());
        verify(bitacora, never()).registrar(any());
    }

    @Test
    void dadoUnContenidoInvalido_cuandoSeRegistra_entoncesEntradaInvalidaSinGuardarNada() {
        Map<String, Object> contenido = contenido();
        contenido.remove("anterior");
        contenido.remove("nuevo");
        EventoRecibido evento =
                new EventoRecibido(
                        UUID.randomUUID(),
                        "generacion.EntradaBitacora",
                        "generacion",
                        Instant.parse("2026-11-06T15:00:01Z"),
                        contenido);

        assertThatThrownBy(() -> servicio.registrar(evento))
                .isInstanceOf(EntradaBitacoraInvalida.class);
        verify(eventos, never()).registrarSiEsNuevo(any());
        verify(bitacora, never()).registrar(any());
    }

    @Test
    void dadoUnTokenAnidadoEnElValorNuevo_cuandoSeRegistra_entoncesSeRechazaSinGuardarNada() {
        Map<String, Object> contenido = contenido();
        contenido.put("nuevo", Map.of("horas", "6", "sesion", Map.of("token", "abc")));
        EventoRecibido evento =
                new EventoRecibido(
                        UUID.randomUUID(),
                        "roles.EntradaBitacora",
                        "roles",
                        Instant.parse("2026-11-06T15:00:01Z"),
                        contenido);

        assertThatThrownBy(() -> servicio.registrar(evento))
                .isInstanceOf(EntradaBitacoraInvalida.class)
                .hasMessageNotContaining("abc");
        verify(bitacora, never()).registrar(any());
    }

    private static EventoRecibido evento(String tipo, String origen) {
        return new EventoRecibido(
                UUID.randomUUID(),
                tipo,
                origen,
                Instant.parse("2026-11-06T15:00:01Z"),
                contenido());
    }

    private static Map<String, Object> contenido() {
        Map<String, Object> contenido = new HashMap<>();
        contenido.put("usuario", "coordinacion.demo");
        contenido.put("accion", "ACTIVIDAD_GUARDADA");
        contenido.put("momento", "2026-11-06T15:00:00Z");
        contenido.put("periodo", "2026-I");
        contenido.put("objeto", Map.of("actividad", "TUTORIAS"));
        contenido.put("anterior", Map.of("horas", "4"));
        contenido.put("nuevo", Map.of("horas", "6"));
        return contenido;
    }
}
