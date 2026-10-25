package ec.mdd.docentes.puertos.entrada;

/** Ya existe un proyecto con ese código (F03-H05). */
public class ProyectoRepetido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin el código. */
    public ProyectoRepetido() {
        super("Ya existe un proyecto con ese código");
    }
}
