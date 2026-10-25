package ec.mdd.docentes.puertos.entrada;

/** No existe un proyecto con ese código (F03-H05). */
public class ProyectoNoEncontrado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin el código. */
    public ProyectoNoEncontrado() {
        super("No existe un proyecto con ese código");
    }
}
