package ec.mdd.normativa.puertos.entrada;

import ec.mdd.normativa.dominio.ActividadDelCatalogo;
import java.util.List;

/**
 * El catálogo de la versión que rige un periodo.
 *
 * @param version el número de la versión
 * @param actividades sus actividades, en el orden de la norma
 */
public record ActividadesDeLaVersion(int version, List<ActividadDelCatalogo> actividades) {

    /** Copia la lista. */
    public ActividadesDeLaVersion {
        actividades = List.copyOf(actividades);
    }
}
