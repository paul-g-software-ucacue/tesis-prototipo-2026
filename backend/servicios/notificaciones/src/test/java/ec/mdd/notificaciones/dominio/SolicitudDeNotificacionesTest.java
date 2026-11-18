package ec.mdd.notificaciones.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** El contenido de un evento de notificaciones, validado (F16-H01, data-model.md, FR-014). */
class SolicitudDeNotificacionesTest {

    @Test
    void dadoUnContenidoValido_cuandoSeLee_entoncesTraeSusMensajesConSuDestino() {
        Map<String, Object> contenido = contenido();
        mensajes(contenido).add(mensaje("COORDINADOR_CARRERA", "TI", "/oferta"));

        SolicitudDeNotificaciones solicitud = SolicitudDeNotificaciones.de(contenido);

        assertThat(solicitud.periodo()).isEqualTo("2026-I");
        assertThat(solicitud.autor()).isEqualTo("consolidacion.demo");
        assertThat(solicitud.momento()).isEqualTo(Instant.parse("2026-11-15T15:04:05Z"));
        assertThat(solicitud.mensajes()).hasSize(2);
        MensajeSolicitado primero = solicitud.mensajes().get(0);
        assertThat(primero.destino())
                .isEqualTo(new DestinoDelMensaje.PorRol("VICERRECTOR", Optional.empty()));
        assertThat(primero.titulo())
                .isEqualTo("El distributivo llegó a «Revisión del Vicerrectorado»");
        assertThat(primero.detalle()).isEqualTo("Periodo 2026-I, versión 1.");
        assertThat(primero.enlace()).isEqualTo("/consolidado");
        assertThat(solicitud.mensajes().get(1).destino().carrera()).contains("TI");
    }

    @Test
    void dadoUnCampoQueFaltaOSobra_cuandoSeLee_entoncesEsInvalida() {
        for (String campo : List.of("periodo", "autor", "momento", "mensajes")) {
            Map<String, Object> contenido = contenido();
            contenido.remove(campo);
            assertThatThrownBy(() -> SolicitudDeNotificaciones.de(contenido))
                    .as(campo)
                    .isInstanceOf(NotificacionInvalida.class);
        }
        Map<String, Object> conClave = contenido();
        conClave.put("token", "x");
        assertThatThrownBy(() -> SolicitudDeNotificaciones.de(conClave))
                .isInstanceOf(NotificacionInvalida.class);
        Map<String, Object> conClaveEnMensaje = contenido();
        mensajes(conClaveEnMensaje).get(0).put("contrasena", "x");
        assertThatThrownBy(() -> SolicitudDeNotificaciones.de(conClaveEnMensaje))
                .isInstanceOf(NotificacionInvalida.class);
    }

    @Test
    void dadoUnPeriodoUnAutorOUnMomentoFueraDeSuForma_cuandoSeLee_entoncesEsInvalida() {
        assertInvalida(Map.of("periodo", "2026 I"));
        assertInvalida(Map.of("autor", ""));
        assertInvalida(Map.of("autor", "a".repeat(65)));
        assertInvalida(Map.of("momento", "ayer"));
    }

    @Test
    void dadosCeroOMasDeCincuentaMensajes_cuandoSeLee_entoncesEsInvalida() {
        Map<String, Object> sinMensajes = contenido();
        mensajes(sinMensajes).clear();
        assertThatThrownBy(() -> SolicitudDeNotificaciones.de(sinMensajes))
                .isInstanceOf(NotificacionInvalida.class);
        Map<String, Object> muchos = contenido();
        for (int i = 0; i < 50; i++) {
            mensajes(muchos).add(mensaje("VICERRECTOR", null, "/consolidado"));
        }
        assertThatThrownBy(() -> SolicitudDeNotificaciones.de(muchos))
                .isInstanceOf(NotificacionInvalida.class);
    }

    @Test
    void dadoUnMensajeFueraDeSuForma_cuandoSeLee_entoncesEsInvalida() {
        assertMensajeInvalido("titulo", "   ");
        assertMensajeInvalido("titulo", "t".repeat(201));
        assertMensajeInvalido("detalle", "d".repeat(1001));
        assertMensajeInvalido("enlace", "https://otro.sitio");
        assertMensajeInvalido("enlace", "/Consolidado");
        assertMensajeInvalido("enlace", "//");
        assertMensajeInvalido("enlace", "/oferta/");
        assertDestinoInvalido(Map.of("rol", "JEFE"));
        assertDestinoInvalido(Map.of("rol", "COORDINADOR_CARRERA", "carrera", "ti"));
        assertDestinoInvalido(Map.of());
    }

    @Test
    void dadoUnDestinoPorUsuarioOPorIdentificacion_cuandoSeLee_entoncesEsLaPersona() {
        Map<String, Object> contenido = contenido();
        mensajes(contenido).get(0).put("destino", new HashMap<>(Map.of("usuario", "docente.demo")));
        Map<String, Object> segundo = mensaje("VICERRECTOR", null, "/firmas-pendientes");
        segundo.put("destino", new HashMap<>(Map.of("identificacion", "0109990010")));
        mensajes(contenido).add(segundo);

        SolicitudDeNotificaciones solicitud = SolicitudDeNotificaciones.de(contenido);

        assertThat(solicitud.mensajes())
                .extracting(MensajeSolicitado::destino)
                .containsExactly(
                        new DestinoDelMensaje.PorUsuario("docente.demo"),
                        new DestinoDelMensaje.PorIdentificacion("0109990010"));
        assertThat(solicitud.mensajes().get(1).destino().carrera()).isEmpty();
    }

    @Test
    void dadoUnDestinoConDosClavesOConCarreraSinRol_cuandoSeLee_entoncesEsInvalida() {
        assertDestinoInvalido(Map.of("rol", "VICERRECTOR", "usuario", "vicerrectorado.demo"));
        assertDestinoInvalido(Map.of("usuario", "docente.demo", "identificacion", "0109990010"));
        assertDestinoInvalido(Map.of("rol", "RECTOR", "identificacion", "0109990010"));
        assertDestinoInvalido(Map.of("usuario", "docente.demo", "carrera", "TI"));
        assertDestinoInvalido(Map.of("identificacion", "0109990010", "carrera", "TI"));
    }

    @Test
    void dadoUnUsuarioOUnaIdentificacionFueraDeSuForma_cuandoSeLee_entoncesEsInvalida() {
        assertDestinoInvalido(Map.of("usuario", ""));
        assertDestinoInvalido(Map.of("usuario", "u".repeat(65)));
        assertDestinoInvalido(Map.of("identificacion", "01099900101234"));
        assertDestinoInvalido(Map.of("identificacion", "0109-990010"));
    }

    @Test
    void dadaUnaIdentificacionInvalida_cuandoSeRechaza_entoncesElMensajeNoLaRepite() {
        Map<String, Object> contenido = contenido();
        mensajes(contenido)
                .get(0)
                .put("destino", new HashMap<>(Map.of("identificacion", "ab-1234567")));

        assertThatThrownBy(() -> SolicitudDeNotificaciones.de(contenido))
                .isInstanceOf(NotificacionInvalida.class)
                .message()
                .doesNotContain("ab-1234567");
    }

    @Test
    void dadoUnDestinoPorUsuarioDeSesentaYCuatro_cuandoSeLee_entoncesSeAcepta() {
        Map<String, Object> contenido = contenido();
        mensajes(contenido).get(0).put("destino", new HashMap<>(Map.of("usuario", "u".repeat(64))));

        assertThat(SolicitudDeNotificaciones.de(contenido).mensajes().get(0).destino())
                .isEqualTo(new DestinoDelMensaje.PorUsuario("u".repeat(64)));
    }

    @Test
    void dadoUnDetalleVacioYElLimiteDeCadaLargo_cuandoSeLee_entoncesSeAcepta() {
        Map<String, Object> contenido = contenido();
        Map<String, Object> mensaje = mensajes(contenido).get(0);
        mensaje.put("titulo", "t".repeat(200));
        mensaje.put("detalle", "");
        mensaje.put("enlace", "/firmas-pendientes");

        SolicitudDeNotificaciones solicitud = SolicitudDeNotificaciones.de(contenido);

        assertThat(solicitud.mensajes().get(0).detalle()).isEmpty();
    }

    @Test
    void dadoUnContenidoDeMasDe64KB_cuandoSeLee_entoncesEsInvalida() {
        Map<String, Object> contenido = contenido();
        for (int i = 0; i < 49; i++) {
            Map<String, Object> mensaje = mensaje("VICERRECTOR", null, "/consolidado");
            mensaje.put("detalle", "d".repeat(1000));
            mensaje.put("titulo", "t".repeat(200));
            mensajes(contenido).add(mensaje);
        }
        Map<String, Object> grande = contenido;
        mensajes(grande).forEach(m -> m.put("detalle", "ñ".repeat(1000)));

        assertThatThrownBy(() -> SolicitudDeNotificaciones.de(grande))
                .isInstanceOf(NotificacionInvalida.class)
                .hasMessageContaining("64 KB");
    }

    private static void assertInvalida(Map<String, Object> cambio) {
        Map<String, Object> contenido = contenido();
        contenido.putAll(cambio);
        assertThatThrownBy(() -> SolicitudDeNotificaciones.de(contenido))
                .as(cambio.toString())
                .isInstanceOf(NotificacionInvalida.class);
    }

    private static void assertMensajeInvalido(String campo, String valor) {
        Map<String, Object> contenido = contenido();
        mensajes(contenido).get(0).put(campo, valor);
        assertThatThrownBy(() -> SolicitudDeNotificaciones.de(contenido))
                .as(campo + "=" + valor)
                .isInstanceOf(NotificacionInvalida.class);
    }

    private static void assertDestinoInvalido(Map<String, Object> destino) {
        Map<String, Object> contenido = contenido();
        mensajes(contenido).get(0).put("destino", new HashMap<>(destino));
        assertThatThrownBy(() -> SolicitudDeNotificaciones.de(contenido))
                .as(destino.toString())
                .isInstanceOf(NotificacionInvalida.class);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mensajes(Map<String, Object> contenido) {
        return (List<Map<String, Object>>) contenido.get("mensajes");
    }

    static Map<String, Object> contenido() {
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("periodo", "2026-I");
        contenido.put("autor", "consolidacion.demo");
        contenido.put("momento", "2026-11-15T15:04:05Z");
        List<Map<String, Object>> mensajes = new ArrayList<>();
        Map<String, Object> mensaje = mensaje("VICERRECTOR", null, "/consolidado");
        mensajes.add(mensaje);
        contenido.put("mensajes", mensajes);
        return contenido;
    }

    static Map<String, Object> mensaje(String rol, String carrera, String enlace) {
        Map<String, Object> destino = new LinkedHashMap<>();
        destino.put("rol", rol);
        if (carrera != null) {
            destino.put("carrera", carrera);
        }
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("destino", destino);
        mensaje.put("titulo", "El distributivo llegó a «Revisión del Vicerrectorado»");
        mensaje.put("detalle", "Periodo 2026-I, versión 1.");
        mensaje.put("enlace", enlace);
        return mensaje;
    }
}
