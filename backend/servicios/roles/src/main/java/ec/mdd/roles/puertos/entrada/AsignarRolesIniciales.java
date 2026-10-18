package ec.mdd.roles.puertos.entrada;

import java.util.Collection;

/**
 * Caso de uso: da a un usuario de la demo los roles de su archivo, solo la primera vez (FR-009,
 * research §8). Después, solo el administrador cambia sus roles, aunque se los quite todos.
 */
public interface AsignarRolesIniciales {

    /**
     * @param usuario nombre de usuario; se normaliza
     * @param roles identificadores de los roles iniciales
     * @return {@code true} si los asignó; {@code false} si el usuario ya los había recibido
     * @throws RolDesconocido si algún rol no existe; entonces no asigna ninguno
     */
    boolean asignar(String usuario, Collection<String> roles);
}
