package ec.mdd.normativa.puertos.salida;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Un cambio de la normativa, con quién lo hizo y cuándo, para la bitácora (FR-009, principio VII).
 *
 * @param momento cuándo
 * @param usuario quién; {@code sistema} para la versión 1
 * @param accion qué
 * @param detalle sobre qué: el número de la versión y lo que cambió
 */
public record CambioDeNormativa(
        Instant momento, String usuario, Accion accion, Map<String, Object> detalle) {

    /** Exige todo y copia el detalle. */
    public CambioDeNormativa {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(accion, "accion");
        detalle = Map.copyOf(detalle);
    }

    /** Las acciones que se registran (columna {@code accion}). */
    public enum Accion {
        /** Versión creada a partir de otra. */
        VERSION_CREADA,
        /** Versión editada, con cada valor anterior y nuevo. */
        VERSION_EDITADA,
        /** Versión asociada a un periodo. */
        VERSION_ASOCIADA
    }
}
