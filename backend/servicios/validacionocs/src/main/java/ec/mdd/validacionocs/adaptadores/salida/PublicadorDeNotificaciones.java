package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import ec.mdd.validacionocs.dominio.MensajeDeTransicion;
import ec.mdd.validacionocs.dominio.MensajesDeTransicion;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.salida.NombresDeCarreras;
import ec.mdd.validacionocs.puertos.salida.NotificacionDeTransiciones;
import java.time.Clock;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica cada transición como el evento {@value #TIPO} hacia Notificaciones, con el contenido
 * {@code SolicitudDeNotificaciones} de {@code contratos/notificaciones} (F16-H01, D-108). El evento
 * lo guarda el registro persistente de publicaciones en la transacción en curso, y se entrega
 * después de confirmarla (ADR-018): una transición revertida no notifica, y una confirmada no se
 * pierde.
 *
 * <p>Si algún mensaje nombra una carrera, pide los nombres a Académico. Si Académico no responde,
 * el título lleva el código: notificar nunca hace fallar la transición.
 */
public final class PublicadorDeNotificaciones implements NotificacionDeTransiciones {

    /** El tipo del evento. */
    public static final String TIPO = "validacionocs.NotificacionesSolicitadas";

    private static final String ORIGEN = "validacionocs";
    private static final String DESTINO = "notificaciones";

    private static final Logger LOG = LoggerFactory.getLogger(PublicadorDeNotificaciones.class);
    private static final Set<TipoDeTransicion> NOMBRAN_CARRERA =
            EnumSet.of(TipoDeTransicion.ENVIO_PARCIAL, TipoDeTransicion.DEVOLUCION);

    private final ApplicationEventPublisher eventos;
    private final NombresDeCarreras carreras;
    private final Clock reloj;

    /**
     * Publicador sobre el {@link ApplicationEventPublisher} del contexto.
     *
     * @param eventos el publicador de Spring, que guarda el evento en la transacción en curso
     * @param carreras el nombre de cada carrera, en Académico
     * @param reloj el momento de la publicación
     */
    public PublicadorDeNotificaciones(
            ApplicationEventPublisher eventos, NombresDeCarreras carreras, Clock reloj) {
        this.eventos = eventos;
        this.carreras = carreras;
        this.reloj = reloj;
    }

    @Override
    public void notificar(ProcesoAprobacion proceso, List<Transicion> nuevas) {
        Map<String, String> nombres = nombresPara(nuevas);
        for (Transicion transicion : nuevas) {
            Map<String, Object> solicitud = new LinkedHashMap<>();
            solicitud.put("periodo", proceso.periodo());
            solicitud.put("autor", transicion.quien());
            solicitud.put("momento", transicion.momento().toString());
            solicitud.put(
                    "mensajes",
                    MensajesDeTransicion.de(proceso, transicion, nombres).stream()
                            .map(PublicadorDeNotificaciones::mensaje)
                            .toList());
            eventos.publishEvent(
                    EventoEntreProcesos.nuevo(TIPO, ORIGEN, DESTINO, solicitud, reloj));
        }
    }

    private Map<String, String> nombresPara(List<Transicion> nuevas) {
        if (nuevas.stream().noneMatch(transicion -> NOMBRAN_CARRERA.contains(transicion.tipo()))) {
            return Map.of();
        }
        try {
            return carreras.nombres();
        } catch (RuntimeException e) {
            LOG.warn(
                    "Sin los nombres de las carreras, las notificaciones llevan el código: {}",
                    e.getMessage());
            return Map.of();
        }
    }

    private static Map<String, Object> mensaje(MensajeDeTransicion mensaje) {
        Map<String, Object> destino = new LinkedHashMap<>();
        destino.put("rol", mensaje.destino().rol());
        mensaje.destino().carrera().ifPresent(carrera -> destino.put("carrera", carrera));
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("destino", destino);
        contenido.put("titulo", mensaje.titulo());
        contenido.put("detalle", mensaje.detalle());
        contenido.put("enlace", mensaje.enlace());
        return contenido;
    }
}
