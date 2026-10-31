package ec.mdd.generacion.dominio;

import java.util.Objects;

/**
 * Los pesos de la versión de normativa que rige un periodo.
 *
 * @param version el número de la versión
 * @param pesos sus pesos de afinidad
 */
public record PesosDelPeriodo(int version, PesosAfinidad pesos) {

    /** Exige los pesos. */
    public PesosDelPeriodo {
        Objects.requireNonNull(pesos, "pesos");
    }
}
