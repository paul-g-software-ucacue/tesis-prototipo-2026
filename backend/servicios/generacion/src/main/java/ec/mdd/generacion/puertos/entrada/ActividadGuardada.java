package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import java.util.Objects;

/**
 * Una actividad guardada.
 *
 * @param actividad la actividad
 */
public record ActividadGuardada(ActividadNoLectiva actividad) {

    /** Exige la actividad. */
    public ActividadGuardada {
        Objects.requireNonNull(actividad, "actividad");
    }
}
