package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import java.util.Objects;

/**
 * Una actividad guardada, con el aviso de las horas (D-78).
 *
 * @param actividad la actividad
 * @param horasSuficientes si al docente le alcanzan las horas
 */
public record ActividadGuardada(ActividadNoLectiva actividad, boolean horasSuficientes) {

    /** Exige la actividad. */
    public ActividadGuardada {
        Objects.requireNonNull(actividad, "actividad");
    }
}
