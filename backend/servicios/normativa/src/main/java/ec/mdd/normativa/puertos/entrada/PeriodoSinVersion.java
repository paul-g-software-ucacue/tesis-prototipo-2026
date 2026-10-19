package ec.mdd.normativa.puertos.entrada;

/** El periodo no tiene una versión de normativa asociada. */
public class PeriodoSinVersion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param periodo el código del periodo
     */
    public PeriodoSinVersion(String periodo) {
        super("El periodo " + periodo + " no tiene una versión de normativa");
    }
}
