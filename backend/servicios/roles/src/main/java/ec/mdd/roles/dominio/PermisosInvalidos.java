package ec.mdd.roles.dominio;

/**
 * Los permisos de un rol no son válidos: un rol sin permisos, una funcionalidad que no existe, un
 * tipo que la funcionalidad no admite o un permiso repetido. El mensaje está en español y se puede
 * mostrar como detalle del campo {@code permisos}.
 */
public class PermisosInvalidos extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje qué está mal, en español
     */
    public PermisosInvalidos(String mensaje) {
        super(mensaje);
    }
}
