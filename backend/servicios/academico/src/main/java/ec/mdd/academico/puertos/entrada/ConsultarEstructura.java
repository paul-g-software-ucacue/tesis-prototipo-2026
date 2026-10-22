package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.GestionAutorizada;
import ec.mdd.academico.dominio.UnidadOrganica;
import java.util.List;

/**
 * Caso de uso: consultar la estructura orgánica y el catálogo de gestiones autorizadas (FR-005,
 * FR-010, F03-H01). Refleja las operaciones {@code listarUnidades}, {@code consultarUnidad} y
 * {@code listarGestionesAutorizadas} del contrato de Académico. Es parte de la API publicada del
 * servicio: las mallas (F03-H02), los docentes (F03-H04) y las cargas (F01) la usarán.
 */
public interface ConsultarEstructura {

    /** Todas las unidades, activas e inactivas, por código. */
    List<UnidadOrganica> unidades();

    /**
     * @throws UnidadNoEncontrada si no hay una unidad con ese código
     */
    UnidadOrganica unidad(String codigo);

    /** Las 21 gestiones autorizadas de POL 6, por código. */
    List<GestionAutorizada> gestionesAutorizadas();
}
