package ec.mdd.generacion.adaptadores.salida;

import ec.mdd.generacion.puertos.salida.EventoSaliente;
import ec.mdd.generacion.puertos.salida.PublicadorDeEventos;
import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica el evento como un {@link EventoEntreProcesos} con el publicador de eventos de Spring. El
 * registro persistente de publicaciones lo guarda en la transacción en curso, y el oyente de {@code
 * plataforma:eventos} lo entrega después de confirmarla (ADR-018).
 */
public final class PublicadorDeEventosSpring implements PublicadorDeEventos {

    private static final String ORIGEN = "generacion";

    private final ApplicationEventPublisher eventos;

    /** Publicador sobre el {@link ApplicationEventPublisher} del contexto. */
    public PublicadorDeEventosSpring(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
    }

    @Override
    public void publicar(EventoSaliente evento) {
        eventos.publishEvent(
                new EventoEntreProcesos(
                        evento.idEvento(),
                        evento.tipo(),
                        ORIGEN,
                        evento.destino(),
                        evento.momento(),
                        evento.contenido()));
    }
}
