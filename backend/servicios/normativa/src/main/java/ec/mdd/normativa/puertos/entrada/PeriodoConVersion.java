package ec.mdd.normativa.puertos.entrada;

/**
 * El periodo ya tiene otra versión de normativa: un periodo tiene una sola (modelo de dominio
 * §5.1). Está junto a {@link AsociarVersionAPeriodo}, que la lanza, para que Académico la vea.
 */
public class PeriodoConVersion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param periodo el código del periodo
     * @param version la versión que ya lo rige
     */
    public PeriodoConVersion(String periodo, int version) {
        super("El periodo " + periodo + " ya tiene la versión de normativa " + version);
    }
}
