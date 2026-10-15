package ec.mdd.plataforma.eventos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * La tarea programada que reenvía las publicaciones fallidas (principio V, ADR-018).
 *
 * <p>Solo reenvía las {@code FAILED}: una entrega en curso no se duplica. Las que quedaron a medias
 * por una caída del proceso ({@code PUBLISHED} o {@code PROCESSING} viejas) pasan antes a {@code
 * FAILED} con el monitor de publicaciones atascadas de Spring Modulith ({@code
 * spring.modulith.events.staleness.*}). No hay máximo de intentos.
 */
public class ReenvioDePublicaciones {

    private static final Logger LOG = LoggerFactory.getLogger(ReenvioDePublicaciones.class);

    private final FailedEventPublications fallidas;

    /** Tarea que reenvía las publicaciones fallidas del registro. */
    public ReenvioDePublicaciones(FailedEventPublications fallidas) {
        this.fallidas = fallidas;
    }

    /** Un ciclo: reenvía todas las fallidas y registra cada una, sin su contenido (FR-006). */
    @Scheduled(
            fixedDelayString = "${mdd.eventos.reenvio.intervalo:PT30S}",
            initialDelayString = "${mdd.eventos.reenvio.intervalo:PT30S}")
    public void reenviar() {
        fallidas.resubmit(ResubmissionOptions.defaults().withFilter(this::registrar));
    }

    boolean registrar(EventPublication publicacion) {
        if (publicacion.getEvent() instanceof EventoEntreProcesos evento) {
            LOG.info("Reenvío del evento {} de tipo {}", evento.idEvento(), evento.tipo());
        } else {
            LOG.info("Reenvío de la publicación {}", publicacion.getIdentifier());
        }
        return true;
    }
}
