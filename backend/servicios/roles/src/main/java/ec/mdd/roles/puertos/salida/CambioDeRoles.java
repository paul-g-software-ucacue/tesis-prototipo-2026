package ec.mdd.roles.puertos.salida;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Un cambio de roles o de asignaciones, con quién lo hizo y cuándo (FR-011).
 *
 * @param momento cuándo
 * @param usuario quién; {@code sistema} para los roles iniciales
 * @param accion qué
 * @param detalle sobre qué: identificadores, nombres de usuario y permisos, nunca datos sensibles
 * @param anterior los valores antes del cambio; vacío al crear
 * @param nuevo los valores después del cambio; vacío al quitar
 */
public record CambioDeRoles(
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
    public CambioDeRoles {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(accion, "accion");
        detalle = Map.copyOf(detalle);
        anterior = Map.copyOf(anterior);
        nuevo = Map.copyOf(nuevo);
        boolean conAnterior = !anterior.isEmpty();
        boolean conNuevo = !nuevo.isEmpty();
        boolean cumple =
                switch (accion.clase()) {
                    case CREA -> !conAnterior && conNuevo;
                    case CAMBIA -> conAnterior && conNuevo;
                    case QUITA -> conAnterior && !conNuevo;
                    case CREA_O_CAMBIA -> conNuevo;
                };
        if (!cumple) {
            throw new IllegalArgumentException(
                    "El cambio " + accion + " no cumple su clase " + accion.clase() + ".");
        }
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
        CREA_O_CAMBIA
    }

    /** Las acciones que se registran (columna {@code accion}). */
    public enum Accion {
        /** Rol creado. */
        CREAR_ROL(Clase.CREA),
        /** Rol editado. */
        EDITAR_ROL(Clase.CAMBIA),
        /** Rol borrado. */
        BORRAR_ROL(Clase.QUITA),
        /** Uno o más roles dados a un usuario, con sus roles antes y después. */
        ASIGNAR_ROL(Clase.CAMBIA),
        /** Uno o más roles quitados a un usuario, con sus roles antes y después. */
        QUITAR_ROL(Clase.CAMBIA),
        /** Roles iniciales de un usuario de la demo (research §8). */
        INICIALIZAR(Clase.CREA),
        /** Carrera indicada a un coordinador, con la que tenía (F06-H05). */
        INDICAR_CARRERA(Clase.CREA_O_CAMBIA),
        /** Carrera quitada a un coordinador (F06-H05). */
        QUITAR_CARRERA(Clase.QUITA),
        /** Responsable de consolidación designado en un periodo, con el anterior (F05-H03). */
        DESIGNAR_RESPONSABLE(Clase.CREA_O_CAMBIA);

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
