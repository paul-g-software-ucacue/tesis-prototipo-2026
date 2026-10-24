package ec.mdd.academico.puertos.salida;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Un cambio de un periodo (F02-H01) o de la estructura orgánica (F03-H01), con quién lo hizo y
 * cuándo, para la bitácora (principio VII).
 *
 * @param momento cuándo
 * @param usuario quién
 * @param accion qué
 * @param detalle sobre qué: el código del periodo o de la unidad, y lo que cambió
 */
public record CambioRegistrado(
        Instant momento, String usuario, Accion accion, Map<String, Object> detalle) {

    /** Exige todo y copia el detalle. */
    public CambioRegistrado {
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
        PERIODO_ESTADO_CAMBIADO,
        /** Unidad registrada, con todos sus datos. */
        UNIDAD_REGISTRADA,
        /** Nombre, padre o datos de su tipo corregidos, con cada valor anterior y nuevo. */
        UNIDAD_CORREGIDA,
        /** Unidad inactivada. */
        UNIDAD_INACTIVADA,
        /** Unidad reactivada. */
        UNIDAD_REACTIVADA,
        /** Carrera registrada, con sus datos principales (F03-H02). */
        CARRERA_REGISTRADA,
        /** Datos de una carrera corregidos, con cada valor anterior y nuevo. */
        CARRERA_CORREGIDA,
        /** Estado de una carrera cambiado, con el anterior y el nuevo. */
        CARRERA_ESTADO_CAMBIADO,
        /** Malla registrada, con su carrera. */
        MALLA_REGISTRADA,
        /** Malla derogada o vuelta a poner vigente, con el estado anterior y el nuevo. */
        MALLA_ESTADO_CAMBIADO,
        /** Asignatura registrada, con su carrera, su malla y sus datos principales. */
        ASIGNATURA_REGISTRADA,
        /** Datos de una asignatura corregidos, con cada valor anterior y nuevo. */
        ASIGNATURA_CORREGIDA,
        /** Asignatura inactivada. */
        ASIGNATURA_INACTIVADA,
        /** Asignatura reactivada. */
        ASIGNATURA_REACTIVADA
    }
}
