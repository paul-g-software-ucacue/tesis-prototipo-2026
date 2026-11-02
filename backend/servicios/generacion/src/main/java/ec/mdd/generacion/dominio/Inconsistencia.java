package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una regla que una asignación no cumple.
 *
 * @param regla la regla
 * @param severidad dura o blanda
 * @param horasQueQuedan lo que todavía le cabe al docente; nunca menos de 0
 */
public record Inconsistencia(
        ReglaDeAsignacion regla, Severidad severidad, BigDecimal horasQueQuedan) {

    /** Exige los datos. */
    public Inconsistencia {
        Objects.requireNonNull(regla, "regla");
        Objects.requireNonNull(severidad, "severidad");
        Objects.requireNonNull(horasQueQuedan, "horasQueQuedan");
    }
}
