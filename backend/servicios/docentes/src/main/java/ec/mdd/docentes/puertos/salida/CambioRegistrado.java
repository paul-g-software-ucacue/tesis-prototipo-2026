package ec.mdd.docentes.puertos.salida;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Un cambio de un docente, con quién lo hizo y cuándo, para la bitácora (FR-010, principio VII).
 *
 * @param momento cuándo
 * @param usuario quién
 * @param accion qué
 * @param detalle sobre qué: la identificación del docente y lo que cambió
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
        /** Docente registrado, con su identificación, su carrera base y su dedicación. */
        DOCENTE_REGISTRADO,
        /** Datos o ficha corregidos, con cada valor anterior y nuevo. */
        DOCENTE_CORREGIDO,
        /** Estado laboral cambiado, con el anterior y el nuevo. */
        DOCENTE_ESTADO_CAMBIADO,
        /** Cargo registrado, con su clave, su unidad o su carrera (F03-H05). */
        CARGO_REGISTRADO,
        /** Cargo corregido o finalizado, con cada valor anterior y nuevo. */
        CARGO_CORREGIDO,
        /** Proyecto registrado, con su código, su tipo y su estado. */
        PROYECTO_REGISTRADO,
        /** Proyecto corregido o con otro estado, con cada valor anterior y nuevo. */
        PROYECTO_CORREGIDO
    }
}
