package ec.mdd.generacion.puertos.salida;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Un cambio del distributivo, con quién lo hizo y cuándo (principio VII).
 *
 * @param momento cuándo
 * @param usuario quién
 * @param accion qué
 * @param detalle sobre qué; nunca el texto de una justificación
 */
public record CambioDeDistributivo(
        Instant momento, String usuario, Accion accion, Map<String, Object> detalle) {

    /** Exige todo y copia el detalle. */
    public CambioDeDistributivo {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(accion, "accion");
        detalle = Map.copyOf(detalle);
    }

    /** Las acciones que se registran (columna {@code accion}). */
    public enum Accion {
        /** Distributivo creado. */
        DISTRIBUTIVO_CREADO,
        /** Propuesta guardada o reemplazada. */
        PROPUESTA_GUARDADA,
        /** Propuesta quitada. */
        PROPUESTA_QUITADA,
        /** Actividad no lectiva guardada o reemplazada. */
        ACTIVIDAD_GUARDADA,
        /** Actividad no lectiva quitada. */
        ACTIVIDAD_QUITADA
    }
}
