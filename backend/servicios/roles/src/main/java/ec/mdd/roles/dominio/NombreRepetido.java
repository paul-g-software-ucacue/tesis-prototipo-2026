package ec.mdd.roles.dominio;

/** Ya existe un rol con ese nombre, o con uno que da el mismo identificador. */
public class NombreRepetido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje detalle para el registro; nunca llega a la respuesta
     */
    public NombreRepetido(String mensaje) {
        super(mensaje);
    }
}
