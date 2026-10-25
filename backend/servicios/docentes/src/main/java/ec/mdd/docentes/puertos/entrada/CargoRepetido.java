package ec.mdd.docentes.puertos.entrada;

/** Ese docente ya tiene ese cargo con esa fecha de designación (F03-H05). */
public class CargoRepetido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin la identificación, que es un dato personal. */
    public CargoRepetido() {
        super("Ya existe ese cargo con esa fecha de designación");
    }
}
