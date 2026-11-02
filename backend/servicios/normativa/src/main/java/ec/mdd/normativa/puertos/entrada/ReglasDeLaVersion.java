package ec.mdd.normativa.puertos.entrada;

import java.math.BigDecimal;

/**
 * Las cifras de las reglas duras de una asignación, en la versión que rige un periodo.
 *
 * @param version el número de la versión
 * @param horasMaximasDocente el tope de horas de un docente (RN-02)
 * @param horasClaseConProyecto el tope de horas de clase de un docente con proyecto (RN-13)
 */
public record ReglasDeLaVersion(
        int version, BigDecimal horasMaximasDocente, BigDecimal horasClaseConProyecto) {}
