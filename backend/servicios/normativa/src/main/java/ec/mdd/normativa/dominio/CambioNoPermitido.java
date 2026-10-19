package ec.mdd.normativa.dominio;

/**
 * Una edición que agrega o quita una fila de la tabla, una regla o un parámetro. En una versión
 * solo cambian los valores; su forma es la de la versión de la que se copió (FR-002, F04-H02).
 */
public class CambioNoPermitido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String campo;
    private final String motivo;

    /**
     * @param campo la fila, la regla o el parámetro, por ejemplo {@code reglas[RN-02]}
     * @param motivo qué está mal, en español
     */
    public CambioNoPermitido(String campo, String motivo) {
        super(campo + ": " + motivo);
        this.campo = campo;
        this.motivo = motivo;
    }

    /** La fila, la regla o el parámetro. */
    public String campo() {
        return campo;
    }

    /** Qué está mal, en español. */
    public String motivo() {
        return motivo;
    }
}
