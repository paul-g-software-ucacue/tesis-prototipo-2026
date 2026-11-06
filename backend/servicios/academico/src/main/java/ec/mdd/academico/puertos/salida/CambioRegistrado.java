package ec.mdd.academico.puertos.salida;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
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
 * @param anterior los valores antes del cambio; vacío al crear. Un valor puede ser {@code null}: el
 *     campo no tenía valor
 * @param nuevo los valores después del cambio; vacío al quitar. Un valor puede ser {@code null}
 */
public record CambioRegistrado(
        Instant momento,
        String usuario,
        Accion accion,
        Map<String, Object> detalle,
        Map<String, Object> anterior,
        Map<String, Object> nuevo) {

    /**
     * Exige todo, copia los mapas y rechaza un cambio que no cumple la clase de su acción.
     *
     * @throws IllegalArgumentException si {@code anterior} o {@code nuevo} no son los de la clase
     */
    public CambioRegistrado {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(accion, "accion");
        detalle = Map.copyOf(detalle);
        anterior = copia(anterior);
        nuevo = copia(nuevo);
        boolean cumple =
                switch (accion.clase()) {
                    case CREA -> anterior.isEmpty() && !nuevo.isEmpty();
                    case CAMBIA -> !anterior.isEmpty() && !nuevo.isEmpty();
                };
        if (!cumple) {
            throw new IllegalArgumentException(
                    "El cambio " + accion + " no cumple su clase " + accion.clase() + ".");
        }
    }

    // Admite valores nulos, que Map.copyOf no admite: «antes no tenía padre».
    private static Map<String, Object> copia(Map<String, Object> valores) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(valores));
    }

    /** Qué valores lleva un cambio según su acción (research §2). En Académico nada se borra. */
    public enum Clase {
        /** Solo {@code nuevo}. */
        CREA,
        /** {@code anterior} y {@code nuevo}. */
        CAMBIA
    }

    /** Las acciones que se registran (columna {@code accion}). */
    public enum Accion {
        /** Periodo registrado, con su código, su nombre y sus fechas. */
        PERIODO_REGISTRADO(Clase.CREA),
        /** Nombre o fechas corregidos, con cada valor anterior y nuevo. */
        PERIODO_CORREGIDO(Clase.CAMBIA),
        /** Periodo abierto con su versión de normativa. */
        PERIODO_ABIERTO(Clase.CAMBIA),
        /** Estado cambiado, con el anterior y el nuevo. */
        PERIODO_ESTADO_CAMBIADO(Clase.CAMBIA),
        /** Unidad registrada, con todos sus datos. */
        UNIDAD_REGISTRADA(Clase.CREA),
        /** Nombre, padre o datos de su tipo corregidos, con cada valor anterior y nuevo. */
        UNIDAD_CORREGIDA(Clase.CAMBIA),
        /** Unidad inactivada. */
        UNIDAD_INACTIVADA(Clase.CAMBIA),
        /** Unidad reactivada. */
        UNIDAD_REACTIVADA(Clase.CAMBIA),
        /** Carrera registrada, con sus datos principales (F03-H02). */
        CARRERA_REGISTRADA(Clase.CREA),
        /** Datos de una carrera corregidos, con cada valor anterior y nuevo. */
        CARRERA_CORREGIDA(Clase.CAMBIA),
        /** Estado de una carrera cambiado, con el anterior y el nuevo. */
        CARRERA_ESTADO_CAMBIADO(Clase.CAMBIA),
        /** Malla registrada, con su carrera. */
        MALLA_REGISTRADA(Clase.CREA),
        /** Malla derogada o vuelta a poner vigente, con el estado anterior y el nuevo. */
        MALLA_ESTADO_CAMBIADO(Clase.CAMBIA),
        /** Asignatura registrada, con su carrera, su malla y sus datos principales. */
        ASIGNATURA_REGISTRADA(Clase.CREA),
        /** Datos de una asignatura corregidos, con cada valor anterior y nuevo. */
        ASIGNATURA_CORREGIDA(Clase.CAMBIA),
        /** Asignatura inactivada. */
        ASIGNATURA_INACTIVADA(Clase.CAMBIA),
        /** Asignatura reactivada. */
        ASIGNATURA_REACTIVADA(Clase.CAMBIA),
        /** Asignatura ofertada en un paralelo del periodo, con sus datos (F03-H03). */
        ASIGNATURA_PARALELO_REGISTRADA(Clase.CREA),
        /** Datos de una asignatura-paralelo corregidos, con cada valor anterior y nuevo. */
        ASIGNATURA_PARALELO_CORREGIDA(Clase.CAMBIA),
        /** Asignatura-paralelo retirada de la oferta. */
        ASIGNATURA_PARALELO_RETIRADA(Clase.CAMBIA),
        /** Asignatura-paralelo vuelta a ofertar. */
        ASIGNATURA_PARALELO_REOFERTADA(Clase.CAMBIA),
        /** Matriculados de una carrera en un periodo registrados, por la carga 10 (F01-H04). */
        MATRICULA_REGISTRADA(Clase.CREA),
        /** Total, fecha de corte o fuente corregidos, por la carga 10. */
        MATRICULA_CORREGIDA(Clase.CAMBIA);

        private final Clase clase;

        Accion(Clase clase) {
            this.clase = clase;
        }

        /** Qué valores lleva un cambio de esta acción. */
        public Clase clase() {
            return clase;
        }
    }
}
