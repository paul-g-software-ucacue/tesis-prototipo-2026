package ec.mdd.academico.puertos.entrada;

/**
 * El periodo ya tiene otra versión de normativa, y no se puede cambiar (FR-003). Traduce la regla
 * de Normativa al lenguaje de Académico: no reenvía su error (principio XIII).
 */
public class PeriodoConOtraVersion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código del periodo
     * @param actual la versión que ya tiene
     */
    public PeriodoConOtraVersion(String codigo, int actual) {
        super("El periodo " + codigo + " ya tiene la versión de normativa " + actual);
    }
}
