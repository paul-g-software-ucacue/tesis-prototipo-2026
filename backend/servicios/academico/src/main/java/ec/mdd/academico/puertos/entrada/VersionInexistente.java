package ec.mdd.academico.puertos.entrada;

/** La versión de normativa elegida para abrir el periodo no existe. */
public class VersionInexistente extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int version;

    /**
     * @param version el número pedido
     */
    public VersionInexistente(int version) {
        super("No existe la versión de normativa " + version);
        this.version = version;
    }

    /** El número pedido. */
    public int version() {
        return version;
    }
}
