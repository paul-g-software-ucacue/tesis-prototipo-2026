package ec.mdd.generacion.puertos.entrada;

import java.util.Objects;
import java.util.Optional;

/**
 * Lo que pide quien quita una actividad.
 *
 * @param periodo el código del periodo
 * @param carrera la carrera dueña del parcial
 * @param docente la identificación del docente
 * @param actividad el código de la actividad
 * @param proyecto el código del proyecto; {@code null} si no tiene
 * @param carreraPropia la única carrera que puede tocar; vacío, cualquiera (D-64)
 * @param quien el usuario que la quita
 */
public record ActividadQuitada(
        String periodo,
        String carrera,
        String docente,
        String actividad,
        String proyecto,
        Optional<String> carreraPropia,
        String quien) {

    /** Exige los datos. */
    public ActividadQuitada {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(docente, "docente");
        Objects.requireNonNull(actividad, "actividad");
        Objects.requireNonNull(carreraPropia, "carreraPropia");
        Objects.requireNonNull(quien, "quien");
    }
}
