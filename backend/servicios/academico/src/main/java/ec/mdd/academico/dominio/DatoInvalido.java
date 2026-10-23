package ec.mdd.academico.dominio;

/**
 * Un dato del periodo (FR-006 de F02-H01) o de una unidad de la estructura (FR-007 de F03-H01) que
 * no cumple su regla, con el campo y el motivo en español.
 */
public class DatoInvalido extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final String campo;
    private final String motivo;

    /**
     * @param campo el campo, por ejemplo {@code codigo}, {@code nombre}, {@code fin} o {@code
     *     padre}
     * @param motivo qué está mal, en español
     */
    public DatoInvalido(String campo, String motivo) {
        super(campo + ": " + motivo);
        this.campo = campo;
        this.motivo = motivo;
    }

    /** El campo. */
    public String campo() {
        return campo;
    }

    /** Qué está mal, en español. */
    public String motivo() {
        return motivo;
    }
}
