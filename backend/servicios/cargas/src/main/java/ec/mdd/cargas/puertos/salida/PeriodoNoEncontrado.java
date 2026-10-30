package ec.mdd.cargas.puertos.salida;

/** El periodo no está en los ciclos académicos de Académico (F01-H05). */
public final class PeriodoNoEncontrado extends RuntimeException {

    private final String periodo;

    /**
     * @param periodo el código que se pidió
     */
    public PeriodoNoEncontrado(String periodo) {
        super("El periodo " + periodo + " no está en los ciclos académicos");
        this.periodo = periodo;
    }

    /** El código que se pidió. */
    public String periodo() {
        return periodo;
    }
}
