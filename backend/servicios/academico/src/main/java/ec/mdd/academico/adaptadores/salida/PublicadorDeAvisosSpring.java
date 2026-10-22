package ec.mdd.academico.adaptadores.salida;

import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.salida.AvisosDelPeriodo;
import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import java.time.Clock;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica cada aviso como el evento {@value #TIPO} hacia Roles, con el publicador de eventos de
 * Spring. El registro persistente de publicaciones lo guarda en la transacción en curso, y el
 * oyente de {@code plataforma:eventos} lo entrega después de confirmarla (ADR-018), como en
 * Generación.
 */
public final class PublicadorDeAvisosSpring implements AvisosDelPeriodo {

    /** El tipo del evento, con su contenido {@code {codigo, estado}}. */
    public static final String TIPO = "academico.PeriodoActualizado";

    private static final String ORIGEN = "academico";
    private static final String DESTINO = "roles";

    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    /** Publicador sobre el {@link ApplicationEventPublisher} del contexto. */
    public PublicadorDeAvisosSpring(ApplicationEventPublisher eventos, Clock reloj) {
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Override
    public void actualizado(PeriodoAcademico periodo) {
        eventos.publishEvent(
                EventoEntreProcesos.nuevo(
                        TIPO,
                        ORIGEN,
                        DESTINO,
                        Map.of("codigo", periodo.codigo(), "estado", periodo.estado().name()),
                        reloj));
    }
}
