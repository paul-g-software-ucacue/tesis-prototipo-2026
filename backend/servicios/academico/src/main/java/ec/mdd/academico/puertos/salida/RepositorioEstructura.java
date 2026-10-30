package ec.mdd.academico.puertos.salida;

import ec.mdd.academico.dominio.GestionAutorizada;
import ec.mdd.academico.dominio.UnidadOrganica;
import java.time.Instant;
import java.util.List;

/**
 * La estructura orgánica y el catálogo de gestiones autorizadas del esquema {@code academico}.
 *
 * <p>{@link #bloquear} solo tiene sentido dentro de una {@link Transaccion}: su candado dura hasta
 * que termina, y así dos cambios de la estructura no se cruzan (data-model.md).
 */
public interface RepositorioEstructura {

    /** Todas las unidades, activas e inactivas, por código. */
    List<UnidadOrganica> todas();

    /** Si hay al menos una unidad orgánica activa (F01-H05). */
    boolean hayActivas();

    /** Las 21 gestiones autorizadas, por código. */
    List<GestionAutorizada> catalogo();

    /** Bloquea la estructura hasta que termine la transacción: otro cambio espera. */
    void bloquear();

    /**
     * Guarda una unidad nueva.
     *
     * @param quien el usuario que la registra
     * @param momento cuándo
     * @return falso si ya hay una unidad con ese código; entonces no guarda nada
     */
    boolean registrar(UnidadOrganica unidad, String quien, Instant momento);

    /** Guarda el nombre, el padre, los datos de su tipo y el estado de una unidad que ya existe. */
    void guardar(UnidadOrganica unidad);
}
