package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.TipoDeProyecto;
import java.util.Set;

/**
 * Un docente con proyectos asignables en un periodo.
 *
 * @param docente su número de identificación
 * @param tipos los tipos de esos proyectos
 */
public record DocenteConProyecto(String docente, Set<TipoDeProyecto> tipos) {

    /** Copia los tipos. */
    public DocenteConProyecto {
        tipos = Set.copyOf(tipos);
    }
}
