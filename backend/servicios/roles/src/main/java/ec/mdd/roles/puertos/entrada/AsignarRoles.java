package ec.mdd.roles.puertos.entrada;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Caso de uso: el administrador da y quita roles a un usuario; un usuario puede tener varios
 * (RF-03). El cambio vale desde la próxima renovación de la sesión del usuario (D-62).
 */
public interface AsignarRoles {

    /** Los roles de cada usuario con al menos uno, ordenados por usuario. */
    Map<String, List<String>> rolesDeUsuarios();

    /**
     * Deja al usuario exactamente con esos roles; una lista vacía le quita todos.
     *
     * @param quien el usuario que hace el cambio
     * @return los roles del usuario, ordenados
     * @throws RolDesconocido si algún rol no existe
     * @throws ec.mdd.roles.dominio.SinAdministrador si el sistema se quedaría sin administrador o
     *     quien hace el cambio perdería el permiso
     */
    List<String> asignar(String usuario, Collection<String> roles, String quien);
}
