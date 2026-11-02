package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Las cifras de las reglas duras en la versión de normativa que rige un periodo (principio VI).
 *
 * @param horasMaximasDocente el tope de horas de un docente (RN-02)
 * @param horasClaseConProyecto el tope de horas de clase de un docente con proyecto (RN-13)
 */
public record ReglasDelPeriodo(BigDecimal horasMaximasDocente, BigDecimal horasClaseConProyecto) {

    /** Exige las dos cifras: sin ellas no se valida. */
    public ReglasDelPeriodo {
        Objects.requireNonNull(horasMaximasDocente, "horasMaximasDocente");
        Objects.requireNonNull(horasClaseConProyecto, "horasClaseConProyecto");
    }

    /** Lo máximo que se le puede asignar: el menor entre sus horas contratadas y el tope. */
    public BigDecimal limite(int horasContratadas) {
        return BigDecimal.valueOf(horasContratadas).min(horasMaximasDocente);
    }
}
