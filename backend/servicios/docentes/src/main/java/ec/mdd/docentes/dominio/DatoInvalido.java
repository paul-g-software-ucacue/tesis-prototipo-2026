package ec.mdd.docentes.dominio;

/**
 * Un dato de un docente o de su ficha académica que no cumple su regla (FR-006), con el campo y el
 * motivo en español. El campo es el del contrato; en las listas de la ficha lleva su posición, como
 * {@code ficha.cursos[1].horas}. El motivo nunca repite el valor: una identificación, un correo o
 * un teléfono son datos personales (principio IX).
 */
public class DatoInvalido extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final String campo;
    private final String motivo;

    /**
     * @param campo el campo, por ejemplo {@code horasContratadas} o {@code ficha.cursos[1].horas}
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
