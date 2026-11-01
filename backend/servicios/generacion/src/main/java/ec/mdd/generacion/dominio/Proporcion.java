package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Los porcentajes de clase y de otras actividades de docencia de un docente (RN-05). Son enteros y
 * siempre suman 100. Investigación, vinculación y gestión no entran.
 *
 * @param clase el porcentaje de horas de clase
 * @param otrasDocencia el porcentaje de otras actividades de docencia
 */
public record Proporcion(int clase, int otrasDocencia) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /**
     * Calcula los porcentajes. El de clase se redondea al entero más cercano, y el otro es lo que
     * falta para 100.
     *
     * @param clase las horas de clase
     * @param otrasDocencia las horas de otras actividades de docencia
     * @return vacío si no hay horas en ninguna
     */
    public static Optional<Proporcion> de(BigDecimal clase, BigDecimal otrasDocencia) {
        BigDecimal base = clase.add(otrasDocencia);
        if (base.signum() == 0) {
            return Optional.empty();
        }
        int deClase =
                clase.multiply(CIEN)
                        .divide(base, MathContext.DECIMAL64)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValueExact();
        return Optional.of(new Proporcion(deClase, 100 - deClase));
    }
}
