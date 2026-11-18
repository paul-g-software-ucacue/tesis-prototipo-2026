package ec.mdd.firma.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.firma.dominio.FirmanteEsperado;
import ec.mdd.firma.puertos.salida.FirmaPendienteSolicitada;
import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * La notificación de firma pendiente se publica como {@code firma.NotificacionesSolicitadas} hacia
 * Notificaciones, con el contenido {@code SolicitudDeNotificaciones} y el destino por persona
 * (F16-H02, contracts/eventos.md).
 */
class PublicadorDeFirmaPendienteTest {

    private static final Instant ACEPTADA = Instant.parse("2026-11-18T15:04:05Z");
    private static final Instant PUBLICADO = Instant.parse("2026-11-18T15:04:06Z");

    private final List<Object> publicados = new ArrayList<>();
    private final PublicadorDeFirmaPendiente publicador =
            new PublicadorDeFirmaPendiente(publicados::add, Clock.fixed(PUBLICADO, ZoneOffset.UTC));

    @Test
    void dadoElSiguienteFirmantePorIdentificacion_cuandoSeNotifica_entoncesPublicaSuSolicitud() {
        publicador.notificar(
                solicitud(
                        FirmanteEsperado.porIdentificacion(
                                2, "VICERRECTOR", "0109990028", "Vicerrector Dos"),
                        "Revisado por"));

        assertThat(publicados).hasSize(1);
        EventoEntreProcesos evento = (EventoEntreProcesos) publicados.getFirst();
        assertThat(evento.tipo()).isEqualTo("firma.NotificacionesSolicitadas");
        assertThat(evento.origen()).isEqualTo("firma");
        assertThat(evento.destino()).isEqualTo("notificaciones");
        assertThat(evento.momento()).isEqualTo(PUBLICADO);
        assertThat(evento.contenido())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "periodo", "2026-I",
                                "autor", "docente.demo",
                                "momento", "2026-11-18T15:04:05Z",
                                "mensajes",
                                        List.of(
                                                Map.of(
                                                        "destino",
                                                        Map.of("identificacion", "0109990028"),
                                                        "titulo",
                                                        "Tienes una firma pendiente: «Revisado"
                                                                + " por»",
                                                        "detalle",
                                                        "Periodo 2026-I, versión 1. «Elaborado"
                                                                + " por» la firmó Docente Uno.",
                                                        "enlace",
                                                        "/firmas-pendientes"))));
    }

    @Test
    void dadoElSiguienteFirmantePorUsuario_cuandoSeNotifica_entoncesElDestinoEsSuUsuario() {
        publicador.notificar(
                solicitud(
                        FirmanteEsperado.porUsuario(
                                2, "RESPONSABLE_CONSOLIDACION", "otro.demo", "Otro"),
                        "Revisado por"));

        EventoEntreProcesos evento = (EventoEntreProcesos) publicados.getFirst();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mensajes =
                (List<Map<String, Object>>) evento.contenido().get("mensajes");
        assertThat(mensajes.getFirst().get("destino")).isEqualTo(Map.of("usuario", "otro.demo"));
    }

    private static FirmaPendienteSolicitada solicitud(FirmanteEsperado firmante, String rotulo) {
        return new FirmaPendienteSolicitada(
                "2026-I",
                1,
                "docente.demo",
                ACEPTADA,
                rotulo,
                firmante,
                "Elaborado por",
                "Docente Uno");
    }
}
