package ec.mdd.reporting.dominio;

/**
 * Las inconsistencias abiertas de una versión cuando Generación guardó su contenido congelado
 * (F19-H02, D-120): las duras y las blandas sin justificar (MD-07).
 *
 * @param duras las duras, desde 0
 * @param blandas las blandas sin justificar, desde 0
 */
public record InconsistenciasAbiertas(int duras, int blandas) {

    /** Exige números desde 0. */
    public InconsistenciasAbiertas {
        if (duras < 0 || blandas < 0) {
            throw new IllegalArgumentException("Las inconsistencias abiertas van desde 0");
        }
    }

    /** Si no queda ninguna abierta. */
    public boolean ninguna() {
        return duras == 0 && blandas == 0;
    }
}
