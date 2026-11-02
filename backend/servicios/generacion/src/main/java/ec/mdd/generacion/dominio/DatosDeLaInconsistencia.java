package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Las cifras de una inconsistencia global. El texto para quien la lee lo arma el adaptador.
 *
 * @param cantidad lo que se midió; o {@code null}
 * @param referencia el valor esperado, como la meta; o {@code null}
 * @param minimo lo menos que se admite; o {@code null}
 * @param maximo lo más que se admite; o {@code null}
 * @param lista los elementos afectados
 */
public record DatosDeLaInconsistencia(
        BigDecimal cantidad,
        BigDecimal referencia,
        BigDecimal minimo,
        BigDecimal maximo,
        List<String> lista) {

    /** Copia la lista. */
    public DatosDeLaInconsistencia {
        lista = List.copyOf(lista);
    }

    /** Sin cifras. */
    public static DatosDeLaInconsistencia ninguno() {
        return new DatosDeLaInconsistencia(null, null, null, null, List.of());
    }

    /** Solo lo que se midió. */
    public static DatosDeLaInconsistencia cantidad(BigDecimal cantidad) {
        return new DatosDeLaInconsistencia(cantidad, null, null, null, List.of());
    }

    /** Lo que se midió y lo que se admite. */
    public static DatosDeLaInconsistencia rango(
            BigDecimal cantidad, BigDecimal minimo, BigDecimal maximo) {
        return new DatosDeLaInconsistencia(cantidad, null, minimo, maximo, List.of());
    }

    /** Los elementos afectados. */
    public static DatosDeLaInconsistencia lista(List<String> lista) {
        return new DatosDeLaInconsistencia(null, null, null, null, lista);
    }
}
