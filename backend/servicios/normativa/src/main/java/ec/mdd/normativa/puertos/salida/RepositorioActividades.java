package ec.mdd.normativa.puertos.salida;

import ec.mdd.normativa.dominio.ActividadDelCatalogo;
import java.util.List;

/** El catálogo de actividades de cada versión de normativa (D-79). */
public interface RepositorioActividades {

    /** Las actividades de la versión, en el orden de la norma. */
    List<ActividadDelCatalogo> deLaVersion(int numero);
}
