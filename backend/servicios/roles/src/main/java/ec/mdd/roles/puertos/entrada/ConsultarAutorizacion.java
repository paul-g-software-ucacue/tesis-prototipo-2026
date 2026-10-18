package ec.mdd.roles.puertos.entrada;

/**
 * Caso de uso: los roles y los permisos efectivos de un usuario (ADR-021). Usuarios lo usa al
 * iniciar sesión y al renovar el token, así que un cambio de roles vale desde la próxima renovación
 * (D-62). Refleja la operación {@code consultarAutorizacion} del contrato.
 */
public interface ConsultarAutorizacion {

    /**
     * @param usuario nombre de usuario; se normaliza
     * @return sus roles y permisos; las dos listas vacías si no tiene roles
     */
    Autorizacion consultar(String usuario);
}
