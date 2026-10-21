package ec.mdd.academico.dominio;

/** Un dato del periodo que no cumple su regla (FR-006), con el campo y el motivo en español. */
public class DatoInvalido extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final String campo;
    private final String motivo;

    /**
     * @param campo {@code codigo}, {@code nombre} o {@code fin}
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
