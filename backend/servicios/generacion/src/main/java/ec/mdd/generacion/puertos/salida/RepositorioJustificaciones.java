package ec.mdd.generacion.puertos.salida;

import ec.mdd.generacion.dominio.JustificacionDeInconsistencia;
import java.util.List;
import java.util.UUID;

/** Las justificaciones de las inconsistencias globales. Solo se insertan (principio VII). */
public interface RepositorioJustificaciones {

    /** La última de cada tipo y objeto en la versión. */
    List<JustificacionDeInconsistencia> vigentes(UUID version);

    /** Agrega una justificación: la anterior se conserva. */
    void guardar(UUID version, JustificacionDeInconsistencia justificacion);
}
