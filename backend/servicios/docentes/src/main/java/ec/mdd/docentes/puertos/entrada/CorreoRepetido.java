package ec.mdd.docentes.puertos.entrada;

/**
 * Otro docente ya usa ese correo institucional (409, {@code DOCENTES_CORREO_REPETIDO}). El mensaje
 * no repite el valor: es un dato personal (principio IX).
 */
public class CorreoRepetido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin el valor, que es un dato personal. */
    public CorreoRepetido() {
        super("Otro docente ya usa ese correo institucional");
    }
}
