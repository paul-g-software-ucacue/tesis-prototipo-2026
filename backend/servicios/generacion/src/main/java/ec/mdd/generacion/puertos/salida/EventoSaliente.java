package ec.mdd.generacion.puertos.salida;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Un evento que Generación publica para otro proceso (ADR-018).
 *
 * @param idEvento identificador único; el mismo que guarda el servicio
 * @param tipo {@code generacion.<NombreDelEvento>}
 * @param destino el servicio receptor
 * @param momento instante de la publicación
 * @param contenido datos del evento; nunca contraseñas ni tokens
 */
public record EventoSaliente(
        UUID idEvento,
        String tipo,
        String destino,
        Instant momento,
        Map<String, Object> contenido) {

    /** El contenido queda como una copia que no se puede modificar. */
    public EventoSaliente {
        contenido = contenido == null ? Map.of() : Map.copyOf(contenido);
    }
}
