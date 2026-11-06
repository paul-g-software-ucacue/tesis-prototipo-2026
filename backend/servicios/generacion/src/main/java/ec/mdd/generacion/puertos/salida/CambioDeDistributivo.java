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
 * @param anterior los valores antes del cambio; vacío al crear
 * @param nuevo los valores después del cambio; vacío al quitar
 */
public record CambioDeDistributivo(
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
    public CambioDeDistributivo {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(accion, "accion");
        detalle = Map.copyOf(detalle);
        anterior = Map.copyOf(anterior);
        nuevo = Map.copyOf(nuevo);
        accion.clase().exigir(accion, anterior, nuevo);
    }

    /** Qué valores lleva un cambio según su acción (research §2). */
    public enum Clase {
        /** Solo {@code nuevo}. */
        CREA,
        /** {@code anterior} y {@code nuevo}. */
        CAMBIA,
        /** Solo {@code anterior}. */
        QUITA,
        /** Crea la primera vez y cambia después: {@code nuevo}, y {@code anterior} si lo había. */
        CREA_O_CAMBIA;

        private void exigir(
                Accion accion, Map<String, Object> anterior, Map<String, Object> nuevo) {
            boolean conAnterior = !anterior.isEmpty();
            boolean conNuevo = !nuevo.isEmpty();
            boolean cumple =
                    switch (this) {
                        case CREA -> !conAnterior && conNuevo;
                        case CAMBIA -> conAnterior && conNuevo;
                        case QUITA -> conAnterior && !conNuevo;
                        case CREA_O_CAMBIA -> conNuevo;
                    };
            if (!cumple) {
                throw new IllegalArgumentException(
                        "El cambio " + accion + " no cumple su clase " + this + ".");
            }
        }
    }

    /** Las acciones que se registran (columna {@code accion}). */
    public enum Accion {
        /** Distributivo creado. */
        DISTRIBUTIVO_CREADO(Clase.CREA),
        /** Propuesta guardada o reemplazada. */
        PROPUESTA_GUARDADA(Clase.CREA_O_CAMBIA),
        /** Propuesta quitada. */
        PROPUESTA_QUITADA(Clase.QUITA),
        /** Actividad no lectiva guardada o reemplazada. */
        ACTIVIDAD_GUARDADA(Clase.CREA_O_CAMBIA),
        /** Actividad no lectiva quitada. */
        ACTIVIDAD_QUITADA(Clase.QUITA),
        /** Inconsistencia global justificada, o justificación cambiada. */
        INCONSISTENCIA_JUSTIFICADA(Clase.CREA_O_CAMBIA),
        /** Versión consolidada, o consolidada otra vez. */
        DISTRIBUTIVO_CONSOLIDADO(Clase.CREA_O_CAMBIA),
        /** Informe sustentado adjuntado, o reemplazado. */
        INFORME_ADJUNTADO(Clase.CREA_O_CAMBIA);

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
