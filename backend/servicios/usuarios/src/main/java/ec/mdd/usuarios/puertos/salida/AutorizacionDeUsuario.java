package ec.mdd.usuarios.puertos.salida;

import java.util.List;

/**
 * Los roles y los permisos efectivos de un usuario, tal como van en el token (ADR-021).
 *
 * @param roles identificadores de sus roles, ordenados; vacía si no tiene
 * @param permisos permisos en la forma {@code FUNCIONALIDAD:TIPO[:PROPIO]}, ordenados
 */
public record AutorizacionDeUsuario(List<String> roles, List<String> permisos) {

    /** Sin roles ni permisos. */
    public static final AutorizacionDeUsuario NINGUNA =
            new AutorizacionDeUsuario(List.of(), List.of());

    /** Copia las listas. */
    public AutorizacionDeUsuario {
        roles = List.copyOf(roles);
        permisos = List.copyOf(permisos);
    }
}
