package ec.mdd.roles.puertos.salida;

import ec.mdd.roles.dominio.PeriodoConocido;
import java.time.Instant;
import java.util.Optional;

/**
 * La copia de los periodos académicos que avisa Académico (F05-H03, research §1).
 *
 * <p>{@link #bloquear} solo tiene sentido dentro de una {@link Transaccion}: su candado dura hasta
 * que termina, y serializa dos designaciones del mismo periodo.
 */
public interface RepositorioPeriodosConocidos {

    /** El periodo, si Roles ya lo conoce. */
    Optional<PeriodoConocido> buscar(String codigo);

    /** El periodo, bloqueado hasta que termine la transacción. */
    Optional<PeriodoConocido> bloquear(String codigo);

    /** Guarda la copia nueva del periodo, o la reemplaza. */
    void guardar(PeriodoConocido periodo, Instant momento);
}
