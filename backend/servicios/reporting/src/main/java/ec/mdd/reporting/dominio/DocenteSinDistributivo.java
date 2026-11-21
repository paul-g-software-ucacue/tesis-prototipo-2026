package ec.mdd.reporting.dominio;

/** El docente no está en la versión publicada del periodo (F20-H01, FR-005). */
public class DocenteSinDistributivo extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param periodo el código del periodo
     * @param version la versión publicada
     */
    public DocenteSinDistributivo(String periodo, int version) {
        super("El docente no está en la versión " + version + " del periodo " + periodo + ".");
    }
}
