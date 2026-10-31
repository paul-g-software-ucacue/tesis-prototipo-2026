package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Los pesos y el mínimo de la versión de normativa que rige un periodo.
 *
 * @param version el número de la versión
 * @param pesos sus pesos de afinidad
 * @param minimoAfinidad su mínimo de afinidad (RN-19)
 */
public record PesosDelPeriodo(int version, PesosAfinidad pesos, BigDecimal minimoAfinidad) {

    /** Exige los pesos y el mínimo. */
    public PesosDelPeriodo {
        Objects.requireNonNull(pesos, "pesos");
        Objects.requireNonNull(minimoAfinidad, "minimoAfinidad");
    }
}
