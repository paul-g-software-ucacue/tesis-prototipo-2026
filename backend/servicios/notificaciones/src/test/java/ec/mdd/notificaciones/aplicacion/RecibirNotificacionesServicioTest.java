package ec.mdd.notificaciones.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.notificaciones.dominio.Destinatario;
import ec.mdd.notificaciones.dominio.DestinoDelMensaje;
import ec.mdd.notificaciones.dominio.Notificacion;
import ec.mdd.notificaciones.dominio.NotificacionInvalida;
import ec.mdd.notificaciones.puertos.entrada.EventoRecibido;
import ec.mdd.notificaciones.puertos.salida.Destinatarios;
import ec.mdd.notificaciones.puertos.salida.RepositorioEventosRecibidos;
import ec.mdd.notificaciones.puertos.salida.RepositorioNotificaciones;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * La recepción de notificaciones (F16-H01, data-model.md «Recepción»): una por destinatario activo,
 * sin el autor, una sola vez por evento.
 */
class RecibirNotificacionesServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-15T15:00:05Z");
    private static final String TIPO = "validacionocs.NotificacionesSolicitadas";

    private final RepositorioEventosRecibidos eventos = mock(RepositorioEventosRecibidos.class);
    private final RepositorioNotificaciones notificaciones = mock(RepositorioNotificaciones.class);
    private final Destinatarios destinatarios = mock(Destinatarios.class);
    private final RecibirNotificacionesServicio servicio =
            new RecibirNotificacionesServicio(
                    eventos, notificaciones, destinatarios, Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void nuevo() {
        when(eventos.registrarSiEsNuevo(any())).thenReturn(true);
        when(destinatarios.nombre("consolidacion.demo"))
                .thenReturn(Optional.of("Responsable de consolidación"));
    }

    @Test
    void dadoUnEventoNuevo_cuandoSeRecibe_entoncesUnaNotificacionPorDestinatarioSinElAutor() {
        when(destinatarios.de(new DestinoDelMensaje("VICERRECTOR", Optional.empty()), "2026-I"))
                .thenReturn(
                        List.of(
                                new Destinatario("consolidacion.demo", Optional.empty()),
                                new Destinatario("vicerrectorado.demo", Optional.empty())));
        EventoRecibido evento = evento(mensaje("VICERRECTOR", null));

        servicio.recibir(evento);

        List<Notificacion> guardadas = guardadas();
        assertThat(guardadas)
                .singleElement()
                .satisfies(
                        notificacion -> {
                            assertThat(notificacion.destinatario())
                                    .isEqualTo("vicerrectorado.demo");
                            assertThat(notificacion.idEvento()).isEqualTo(evento.idEvento());
                            assertThat(notificacion.periodo()).isEqualTo("2026-I");
                            assertThat(notificacion.titulo()).isEqualTo("El distributivo llegó");
                            assertThat(notificacion.detalle())
                                    .isEqualTo("Periodo 2026-I, versión 1.");
                            assertThat(notificacion.enlace()).isEqualTo("/consolidado");
                            assertThat(notificacion.autor()).isEqualTo("consolidacion.demo");
                            assertThat(notificacion.autorNombre())
                                    .isEqualTo("Responsable de consolidación");
                            assertThat(notificacion.momento())
                                    .isEqualTo(Instant.parse("2026-11-15T15:00:00Z"));
                            assertThat(notificacion.recibidaEn()).isEqualTo(AHORA);
                            assertThat(notificacion.leida()).isFalse();
                            assertThat(notificacion.carrera()).isEmpty();
                        });
    }

    @Test
    void dadaUnaDevolucion_cuandoSeRecibe_entoncesCadaCoordinadorConLaNotificacionDeSuCarrera() {
        when(destinatarios.de(
                        new DestinoDelMensaje("COORDINADOR_CARRERA", Optional.of("DS")), "2026-I"))
                .thenReturn(List.of(new Destinatario("coordinacion.demo", Optional.of("DS"))));
        when(destinatarios.de(
                        new DestinoDelMensaje("COORDINADOR_CARRERA", Optional.of("TDS")), "2026-I"))
                .thenReturn(
                        List.of(
                                new Destinatario("otra.coordinacion", Optional.of("TDS")),
                                new Destinatario("tercera.coordinacion", Optional.of("TDS"))));

        servicio.recibir(
                evento(
                        mensaje("COORDINADOR_CARRERA", "DS"),
                        mensaje("COORDINADOR_CARRERA", "TDS")));

        assertThat(guardadas())
                .extracting(Notificacion::destinatario, Notificacion::carrera)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("coordinacion.demo", Optional.of("DS")),
                        org.assertj.core.groups.Tuple.tuple(
                                "otra.coordinacion", Optional.of("TDS")),
                        org.assertj.core.groups.Tuple.tuple(
                                "tercera.coordinacion", Optional.of("TDS")));
    }

    @Test
    void dadoElSistemaComoAutor_cuandoSeRecibe_entoncesElAutorEsElSistema() {
        when(destinatarios.de(any(), any()))
                .thenReturn(List.of(new Destinatario("secretaria.demo", Optional.empty())));
        Map<String, Object> contenido = contenido(mensaje("SECRETARIA_GENERAL", null));
        contenido.put("autor", "sistema");

        servicio.recibir(
                new EventoRecibido(UUID.randomUUID(), TIPO, "validacionocs", AHORA, contenido));

        assertThat(guardadas().getFirst().autorNombre()).isEqualTo("el sistema");
    }

    @Test
    void dadoUnAutorSinNombre_cuandoSeRecibe_entoncesUsaSuUsuario() {
        when(destinatarios.nombre("consolidacion.demo")).thenReturn(Optional.empty());
        when(destinatarios.de(any(), any()))
                .thenReturn(List.of(new Destinatario("vicerrectorado.demo", Optional.empty())));

        servicio.recibir(evento(mensaje("VICERRECTOR", null)));

        assertThat(guardadas().getFirst().autorNombre()).isEqualTo("consolidacion.demo");
    }

    @Test
    void dadoUnRolSinUsuarios_cuandoSeRecibe_entoncesNoGuardaNadaNiFalla() {
        when(destinatarios.de(any(), any())).thenReturn(List.of());

        servicio.recibir(evento(mensaje("RECTOR", null)));

        verify(eventos).registrarSiEsNuevo(any());
        verify(notificaciones, never()).guardar(anyList());
    }

    @Test
    void dadoUnEventoRepetido_cuandoSeRecibe_entoncesNoPideDestinatariosNiGuarda() {
        when(eventos.registrarSiEsNuevo(any())).thenReturn(false);

        servicio.recibir(evento(mensaje("VICERRECTOR", null)));

        verify(destinatarios, never()).de(any(), any());
        verify(notificaciones, never()).guardar(anyList());
    }

    @Test
    void dadoUnTipoOUnOrigenNoAdmitido_cuandoSeRecibe_entoncesEventoNoReconocidoSinRegistrar() {
        Map<String, Object> contenido = contenido(mensaje("VICERRECTOR", null));
        for (String[] tipoYOrigen :
                List.of(
                        new String[] {"validacionocs.EntradaBitacora", "validacionocs"},
                        new String[] {TIPO, "generacion"},
                        new String[] {"firma.NotificacionesSolicitadas", "firma"},
                        new String[] {"sin-punto", "validacionocs"})) {
            assertThatThrownBy(
                            () ->
                                    servicio.recibir(
                                            new EventoRecibido(
                                                    UUID.randomUUID(),
                                                    tipoYOrigen[0],
                                                    tipoYOrigen[1],
                                                    AHORA,
                                                    contenido)))
                    .as(tipoYOrigen[0])
                    .isInstanceOf(EventoNoReconocido.class);
        }
        verify(eventos, never()).registrarSiEsNuevo(any());
    }

    @Test
    void dadaUnaSolicitudInvalida_cuandoSeRecibe_entoncesNoRegistraNada() {
        Map<String, Object> contenido = contenido(mensaje("VICERRECTOR", null));
        contenido.remove("periodo");

        assertThatThrownBy(
                        () ->
                                servicio.recibir(
                                        new EventoRecibido(
                                                UUID.randomUUID(),
                                                TIPO,
                                                "validacionocs",
                                                AHORA,
                                                contenido)))
                .isInstanceOf(NotificacionInvalida.class);
        verify(eventos, never()).registrarSiEsNuevo(any());
    }

    @Test
    void dadoQueRolesFalla_cuandoSeRecibe_entoncesPropagaSinGuardar() {
        when(destinatarios.de(any(), any())).thenThrow(new IllegalStateException("Roles"));

        assertThatThrownBy(() -> servicio.recibir(evento(mensaje("VICERRECTOR", null))))
                .isInstanceOf(IllegalStateException.class);
        verify(notificaciones, never()).guardar(anyList());
    }

    private List<Notificacion> guardadas() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notificacion>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificaciones).guardar(captor.capture());
        return captor.getValue();
    }

    @SafeVarargs
    private static EventoRecibido evento(Map<String, Object>... mensajes) {
        return new EventoRecibido(
                UUID.randomUUID(), TIPO, "validacionocs", AHORA, contenido(mensajes));
    }

    @SafeVarargs
    private static Map<String, Object> contenido(Map<String, Object>... mensajes) {
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("periodo", "2026-I");
        contenido.put("autor", "consolidacion.demo");
        contenido.put("momento", "2026-11-15T15:00:00Z");
        contenido.put("mensajes", new ArrayList<>(List.of(mensajes)));
        return contenido;
    }

    private static Map<String, Object> mensaje(String rol, String carrera) {
        Map<String, Object> destino = new LinkedHashMap<>();
        destino.put("rol", rol);
        if (carrera != null) {
            destino.put("carrera", carrera);
        }
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("destino", destino);
        mensaje.put("titulo", "El distributivo llegó");
        mensaje.put("detalle", "Periodo 2026-I, versión 1.");
        mensaje.put("enlace", "/consolidado");
        return mensaje;
    }
}
