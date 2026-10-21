package ec.mdd.academico.puertos.salida;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Un cambio de un periodo, con quién lo hizo y cuándo, para la bitácora (FR-008, principio VII).
 *
 * @param momento cuándo
 * @param usuario quién
 * @param accion qué
 * @param detalle sobre qué: el código del periodo y lo que cambió
 */
public record CambioDePeriodo(
        Instant momento, String usuario, Accion accion, Map<String, Object> detalle) {

    /** Exige todo y copia el detalle. */
    public CambioDePeriodo {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(accion, "accion");
        detalle = Map.copyOf(detalle);
    }

    /** Las acciones que se registran (columna {@code accion}). */
    public enum Accion {
        /** Periodo registrado, con su código, su nombre y sus fechas. */
        PERIODO_REGISTRADO,
        /** Nombre o fechas corregidos, con cada valor anterior y nuevo. */
        PERIODO_CORREGIDO,
        /** Periodo abierto con su versión de normativa. */
        PERIODO_ABIERTO,
        /** Estado cambiado, con el anterior y el nuevo. */
        PERIODO_ESTADO_CAMBIADO
    }
}
