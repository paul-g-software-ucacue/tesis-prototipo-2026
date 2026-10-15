package ec.mdd.usuarios.puertos.entrada;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Un evento que llega de otro proceso, con la forma del sobre de {@code
 * contratos/comun/eventos.yaml} (ADR-018).
 *
 * @param idEvento identificador del evento; clave de idempotencia
 * @param tipo {@code <servicio>.<NombreDelEvento>}
 * @param origen servicio que lo publicó
 * @param momento instante de la publicación
 * @param contenido datos del evento según su tipo
 */
public record EventoRecibido(
        UUID idEvento, String tipo, String origen, Instant momento, Map<String, Object> contenido) {

    /** El contenido queda como una copia que no se puede modificar. */
    public EventoRecibido {
        contenido = contenido == null ? Map.of() : Map.copyOf(contenido);
    }

    /** Sin el contenido, para que no llegue al registro del proceso. */
    @Override
    public String toString() {
        return "EventoRecibido[idEvento="
                + idEvento
                + ", tipo="
                + tipo
                + ", origen="
                + origen
                + "]";
    }
}
