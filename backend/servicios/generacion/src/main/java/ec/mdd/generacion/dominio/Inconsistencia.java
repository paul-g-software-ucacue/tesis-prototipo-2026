package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una regla que una asignación no cumple.
 *
 * @param regla la regla
 * @param horasQueQuedan lo que todavía le cabe al docente; nunca menos de 0
 */
public record Inconsistencia(ReglaDeAsignacion regla, BigDecimal horasQueQuedan) {

    /** Exige los datos. */
    public Inconsistencia {
        Objects.requireNonNull(regla, "regla");
        Objects.requireNonNull(horasQueQuedan, "horasQueQuedan");
    }
}
