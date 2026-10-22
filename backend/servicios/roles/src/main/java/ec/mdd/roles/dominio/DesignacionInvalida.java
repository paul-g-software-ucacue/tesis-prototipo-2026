package ec.mdd.roles.dominio;

/** El usuario no se puede designar Responsable de consolidación (FR-004), con el motivo. */
public class DesignacionInvalida extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final String campo;
    private final String motivo;

    /**
     * @param campo el campo de la solicitud, {@code usuario}
     * @param motivo qué está mal, en español
     */
    public DesignacionInvalida(String campo, String motivo) {
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
