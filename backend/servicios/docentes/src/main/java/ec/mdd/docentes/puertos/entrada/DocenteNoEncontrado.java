package ec.mdd.docentes.puertos.entrada;

/**
 * No hay un docente con esa identificación (404). El mensaje no repite el valor: es un dato
 * personal (principio IX).
 */
public class DocenteNoEncontrado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin el valor, que es un dato personal. */
    public DocenteNoEncontrado() {
        super("No existe un docente con esa identificación");
    }
}
