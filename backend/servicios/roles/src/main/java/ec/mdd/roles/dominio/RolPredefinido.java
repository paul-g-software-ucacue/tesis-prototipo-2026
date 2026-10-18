package ec.mdd.roles.dominio;

/** El rol es uno de los 10 de la matriz D-16, que no se editan ni se borran (D-63). */
public class RolPredefinido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje detalle para el registro; nunca llega a la respuesta
     */
    public RolPredefinido(String mensaje) {
        super(mensaje);
    }
}
