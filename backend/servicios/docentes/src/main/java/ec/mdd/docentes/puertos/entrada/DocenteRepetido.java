package ec.mdd.docentes.puertos.entrada;

/**
 * Ya existe un docente con esa identificación (409, {@code DOCENTES_DOCENTE_REPETIDO}). El mensaje
 * no repite el valor: es un dato personal (principio IX).
 */
public class DocenteRepetido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin el valor, que es un dato personal. */
    public DocenteRepetido() {
        super("Ya existe un docente con esa identificación");
    }
}
