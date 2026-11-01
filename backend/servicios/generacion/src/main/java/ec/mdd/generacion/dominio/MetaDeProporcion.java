package ec.mdd.generacion.dominio;

import java.math.BigDecimal;

/**
 * Los porcentajes de RN-05 en la versión de normativa que rige un periodo. Un docente se compara
 * con ellos, sin que apartarse sea un error (RF-27).
 *
 * @param clase el porcentaje de horas de clase
 * @param otrasDocencia el porcentaje de otras actividades de docencia
 */
public record MetaDeProporcion(BigDecimal clase, BigDecimal otrasDocencia) {}
