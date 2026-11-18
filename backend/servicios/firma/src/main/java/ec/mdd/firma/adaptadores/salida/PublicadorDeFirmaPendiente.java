package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.dominio.FirmanteEsperado;
import ec.mdd.firma.puertos.salida.FirmaPendienteSolicitada;
import ec.mdd.firma.puertos.salida.NotificacionDeFirmaPendiente;
import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica la notificación de firma pendiente como el evento {@value #TIPO} hacia Notificaciones,
 * con el contenido {@code SolicitudDeNotificaciones} de {@code contratos/notificaciones} (F16-H02,
 * D-110). El registro persistente de publicaciones lo guarda en la transacción de la firma y lo
 * entrega después de confirmarla (ADR-018).
 *
 * <p>El destino es la persona del paso siguiente, no su rol: el usuario del designado o la
 * identificación del docente del cargo, como quedaron al generar (D-102). Firma no conoce su
 * cuenta; Notificaciones la resuelve en el proceso {@code identidad}. El autor y el momento son los
 * de la firma aceptada, así que la bandeja ya muestra quién firmó antes y cuándo.
 */
public final class PublicadorDeFirmaPendiente implements NotificacionDeFirmaPendiente {

    /** El tipo del evento. */
    public static final String TIPO = "firma.NotificacionesSolicitadas";

    private static final String ORIGEN = "firma";
    private static final String DESTINO = "notificaciones";
    private static final String ENLACE = "/firmas-pendientes";

    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    /**
     * @param eventos el publicador de Spring, que guarda el evento en la transacción en curso
     * @param reloj el momento de la publicación
     */
    public PublicadorDeFirmaPendiente(ApplicationEventPublisher eventos, Clock reloj) {
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Override
    public void notificar(FirmaPendienteSolicitada solicitud) {
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("destino", destino(solicitud.firmante()));
        mensaje.put("titulo", "Tienes una firma pendiente: «" + solicitud.rotulo() + "»");
        mensaje.put(
                "detalle",
                "Periodo "
                        + solicitud.periodo()
                        + ", versión "
                        + solicitud.version()
                        + ". «"
                        + solicitud.rotuloAnterior()
                        + "» la firmó "
                        + solicitud.nombreAnterior()
                        + ".");
        mensaje.put("enlace", ENLACE);
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("periodo", solicitud.periodo());
        contenido.put("autor", solicitud.autor());
        contenido.put("momento", solicitud.momento().toString());
        contenido.put("mensajes", List.of(mensaje));
        eventos.publishEvent(EventoEntreProcesos.nuevo(TIPO, ORIGEN, DESTINO, contenido, reloj));
    }

    private static Map<String, Object> destino(FirmanteEsperado firmante) {
        return firmante.porIdentificacion()
                ? Map.of("identificacion", firmante.identificacion())
                : Map.of("usuario", firmante.usuario());
    }
}
