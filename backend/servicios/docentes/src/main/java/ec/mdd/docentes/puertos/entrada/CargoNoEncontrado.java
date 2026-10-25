package ec.mdd.docentes.puertos.entrada;

/** No existe un cargo con esa clave (F03-H05). */
public class CargoNoEncontrado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin la identificación, que es un dato personal. */
    public CargoNoEncontrado() {
        super("No existe ese cargo");
    }
}
