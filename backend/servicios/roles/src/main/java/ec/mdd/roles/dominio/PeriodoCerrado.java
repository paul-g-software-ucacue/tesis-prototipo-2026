package ec.mdd.roles.dominio;

/** El periodo está cerrado y ya no admite designaciones (RF-13, F05-H03). */
public class PeriodoCerrado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código del periodo
     */
    public PeriodoCerrado(String codigo) {
        super("El periodo " + codigo + " está cerrado y no admite cambios");
    }
}
