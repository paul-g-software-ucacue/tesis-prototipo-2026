package ec.mdd.validacionocs.dominio;

/**
 * Quedan inconsistencias abiertas al enviar a revisión (F10-H03, MD-07): duras, o blandas sin
 * justificar. Lleva cuántas de cada una, para decirlas en el detalle del error.
 */
public final class InconsistenciasAbiertas extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int duras;
    private final int blandas;

    /**
     * @param duras las duras abiertas
     * @param blandas las blandas sin justificar
     */
    public InconsistenciasAbiertas(int duras, int blandas) {
        super("InconsistenciasAbiertas");
        this.duras = duras;
        this.blandas = blandas;
    }

    /** Las duras abiertas. */
    public int duras() {
        return duras;
    }

    /** Las blandas sin justificar. */
    public int blandas() {
        return blandas;
    }

    /** Todas las abiertas. */
    public int total() {
        return duras + blandas;
    }
}
