package ec.mdd.roles.puertos.entrada;

/** No hay un rol con ese identificador. */
public class RolNoEncontrado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param identificador el identificador buscado
     */
    public RolNoEncontrado(String identificador) {
        super("No existe el rol " + identificador);
    }
}
