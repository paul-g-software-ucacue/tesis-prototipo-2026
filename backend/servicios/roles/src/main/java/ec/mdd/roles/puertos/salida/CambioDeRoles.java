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
 */
public record CambioDeRoles(
        Instant momento, String usuario, Accion accion, Map<String, Object> detalle) {

    /** Exige todo y copia el detalle. */
    public CambioDeRoles {
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(accion, "accion");
        detalle = Map.copyOf(detalle);
    }

    /** Las acciones que se registran (columna {@code accion}). */
    public enum Accion {
        /** Rol creado. */
        CREAR_ROL,
        /** Rol editado. */
        EDITAR_ROL,
        /** Rol borrado. */
        BORRAR_ROL,
        /** Uno o más roles dados a un usuario. */
        ASIGNAR_ROL,
        /** Uno o más roles quitados a un usuario. */
        QUITAR_ROL,
        /** Roles iniciales de un usuario de la demo (research §8). */
        INICIALIZAR
    }
}
