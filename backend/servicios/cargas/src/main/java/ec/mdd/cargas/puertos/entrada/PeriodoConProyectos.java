package ec.mdd.cargas.puertos.entrada;

/**
 * El periodo tiene proyectos asignables, así que no se puede confirmar «sin proyectos» (F01-H05,
 * RF-18). Nada se guarda.
 */
public final class PeriodoConProyectos extends RuntimeException {

    private final String periodo;

    /**
     * @param periodo el código del periodo
     */
    public PeriodoConProyectos(String periodo) {
        super("El periodo " + periodo + " tiene proyectos asignables");
        this.periodo = periodo;
    }

    /** El código del periodo. */
    public String periodo() {
        return periodo;
    }
}
