package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Las horas de actividades por componente. Es una función pura.
 *
 * @param otrasDocencia otras actividades de docencia
 * @param investigacion investigación
 * @param vinculacion vinculación
 * @param gestion gestión
 */
public record SumaPorComponente(
        BigDecimal otrasDocencia,
        BigDecimal investigacion,
        BigDecimal vinculacion,
        BigDecimal gestion) {

    /** Suma las actividades. */
    public static SumaPorComponente de(List<ActividadNoLectiva> actividades) {
        return new SumaPorComponente(
                suma(actividades, Componente.OTRAS_DOCENCIA),
                suma(actividades, Componente.INVESTIGACION),
                suma(actividades, Componente.VINCULACION),
                suma(actividades, Componente.GESTION));
    }

    private static BigDecimal suma(List<ActividadNoLectiva> actividades, Componente componente) {
        return actividades.stream()
                .filter(una -> una.componente() == componente)
                .map(ActividadNoLectiva::horas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
