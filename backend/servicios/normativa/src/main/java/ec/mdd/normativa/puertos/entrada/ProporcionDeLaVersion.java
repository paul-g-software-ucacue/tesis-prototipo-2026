package ec.mdd.normativa.puertos.entrada;

import java.math.BigDecimal;

/**
 * La meta de la proporción de la versión que rige un periodo (RN-05).
 *
 * @param version el número de la versión
 * @param clase el porcentaje de horas de clase
 * @param otrasDocencia el porcentaje de otras actividades de docencia
 */
public record ProporcionDeLaVersion(int version, BigDecimal clase, BigDecimal otrasDocencia) {}
