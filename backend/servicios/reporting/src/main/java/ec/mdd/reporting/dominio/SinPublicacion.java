package ec.mdd.reporting.dominio;

/** El periodo no tiene versión publicada: los reportes salen de ella (F20-H01, FR-005). */
public class SinPublicacion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param periodo el código del periodo
     */
    public SinPublicacion(String periodo) {
        super("El periodo " + periodo + " no tiene versión publicada.");
    }
}
