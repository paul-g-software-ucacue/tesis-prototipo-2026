package ec.mdd.roles.dominio;

/**
 * El cambio dejaría al sistema sin nadie que administre los roles, o se lo quitaría a quien lo
 * hace.
 */
public class SinAdministrador extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje detalle para el registro; nunca llega a la respuesta
     */
    public SinAdministrador(String mensaje) {
        super(mensaje);
    }
}
