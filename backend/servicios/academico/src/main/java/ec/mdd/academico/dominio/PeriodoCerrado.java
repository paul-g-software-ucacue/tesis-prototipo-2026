package ec.mdd.academico.dominio;

/**
 * El periodo está cerrado y ya no admite cambios: ni de datos, ni de estado, ni de versión (RF-13).
 */
public class PeriodoCerrado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código del periodo
     */
    public PeriodoCerrado(String codigo) {
        super("El periodo " + codigo + " está cerrado y no admite cambios");
    }
}
