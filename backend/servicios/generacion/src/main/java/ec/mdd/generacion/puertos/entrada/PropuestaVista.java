package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.Propuesta;
import java.util.Objects;

/**
 * Una propuesta tal como se muestra (RF-24).
 *
 * @param propuesta la propuesta
 * @param retirada si su asignatura-paralelo salió de la oferta
 * @param horasSuficientes si al docente le alcanzan las horas para este paralelo
 */
public record PropuestaVista(Propuesta propuesta, boolean retirada, boolean horasSuficientes) {

    /** Exige la propuesta. */
    public PropuestaVista {
        Objects.requireNonNull(propuesta, "propuesta");
    }
}
