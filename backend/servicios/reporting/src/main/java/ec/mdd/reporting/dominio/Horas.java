package ec.mdd.reporting.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/** Las reglas de las horas semanales del reporte: no negativas y con hasta dos decimales (D-77). */
final class Horas {

    private Horas() {}

    /**
     * @param horas las horas
     * @return las mismas horas
     * @throws IllegalArgumentException si son negativas o tienen más de dos decimales
     */
    static BigDecimal validas(BigDecimal horas) {
        Objects.requireNonNull(horas, "horas");
        if (horas.signum() < 0) {
            throw new IllegalArgumentException("horas negativas: " + horas);
        }
        if (horas.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("horas con más de dos decimales: " + horas);
        }
        return horas;
    }
}
