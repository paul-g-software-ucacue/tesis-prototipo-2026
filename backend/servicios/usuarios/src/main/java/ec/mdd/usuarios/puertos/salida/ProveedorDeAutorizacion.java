package ec.mdd.usuarios.puertos.salida;

import java.util.Collection;

/**
 * Roles y permisos de los usuarios, que son de Roles (research §1 y §2). Usuarios los pide al
 * iniciar sesión y al renovar el token, así que un cambio vale desde la próxima renovación (D-62).
 */
public interface ProveedorDeAutorizacion {

    /**
     * @param nombreUsuario nombre normalizado
     * @return sus roles y permisos; vacíos si no tiene roles
     */
    AutorizacionDeUsuario consultar(String nombreUsuario);

    /**
     * Pide los roles iniciales de un usuario de la demo. Roles los da solo la primera vez: después,
     * los cambia el administrador (FR-009).
     *
     * @throws RuntimeException si algún rol no existe; el mensaje dice cuál
     */
    void asignarRolesIniciales(String nombreUsuario, Collection<String> roles);
}
