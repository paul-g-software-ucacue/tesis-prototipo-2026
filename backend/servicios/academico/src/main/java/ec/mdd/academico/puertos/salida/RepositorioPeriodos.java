package ec.mdd.academico.puertos.salida;

import ec.mdd.academico.dominio.PeriodoAcademico;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Los periodos académicos del esquema {@code academico}.
 *
 * <p>{@link #bloquear} solo tiene sentido dentro de una {@link Transaccion}: su candado dura hasta
 * que termina (research §3).
 */
public interface RepositorioPeriodos {

    /** Todos los periodos, del de inicio más reciente al más antiguo. */
    List<PeriodoAcademico> todos();

    /** Si hay al menos un periodo académico, en cualquier estado (F01-H05). */
    boolean hayPeriodos();

    /** El periodo con ese código, si existe. */
    Optional<PeriodoAcademico> porCodigo(String codigo);

    /**
     * El periodo con ese código, bloqueado hasta que termine la transacción: otro cambio del mismo
     * periodo espera.
     */
    Optional<PeriodoAcademico> bloquear(String codigo);

    /**
     * Bloquea la tabla de los periodos hasta el fin de la transacción, para una carga (F01-H04).
     */
    void bloquearTodos();

    /**
     * Guarda un periodo nuevo.
     *
     * @param quien el usuario que lo registra
     * @param momento cuándo
     * @return falso si ya hay un periodo con ese código; entonces no guarda nada
     */
    boolean registrar(PeriodoAcademico periodo, String quien, Instant momento);

    /** Guarda el nombre, las fechas y el estado de un periodo que ya existe. */
    void guardar(PeriodoAcademico periodo);
}
