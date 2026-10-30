package ec.mdd.academico.puertos.salida;

import ec.mdd.academico.dominio.AsignaturaParalelo;
import java.time.Instant;
import java.util.List;

/**
 * La oferta de cada periodo, del esquema {@code academico} (F03-H03).
 *
 * <p>{@link #bloquear} solo tiene sentido dentro de una {@link Transaccion}: su candado dura hasta
 * que termina, y así la aplicación decide las repetidas con lo que leyó, sin que otro cambio se
 * cruce (research §4).
 */
public interface RepositorioOferta {

    /** Las asignaturas-paralelo del periodo, ofertadas y retiradas, por asignatura y paralelo. */
    List<AsignaturaParalelo> delPeriodo(String periodo);

    /** Si hay al menos una asignatura-paralelo ofertada, en cualquier periodo (F01-H05). */
    boolean hayOfertadas();

    /** Si hay al menos una asignatura-paralelo ofertada en ese periodo (F02-H02). */
    boolean hayOfertadasEn(String periodo);

    /** Bloquea la oferta hasta que termine la transacción. */
    void bloquear();

    /** Guarda una asignatura-paralelo nueva. */
    void registrar(AsignaturaParalelo asignaturaParalelo, String quien, Instant momento);

    /**
     * Guarda los datos y el estado de una asignatura-paralelo que ya existe; el periodo, la
     * asignatura y el paralelo no cambian.
     */
    void guardar(AsignaturaParalelo asignaturaParalelo);
}
