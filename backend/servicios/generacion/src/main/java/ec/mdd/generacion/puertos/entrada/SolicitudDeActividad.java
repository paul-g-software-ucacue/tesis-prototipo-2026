package ec.mdd.generacion.puertos.entrada;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Lo que pide quien asigna una actividad.
 *
 * @param periodo el código del periodo
 * @param carrera la carrera dueña del parcial
 * @param docente la identificación del docente
 * @param actividad el código de la actividad
 * @param horas sus horas semanales
 * @param proyecto el código del proyecto; {@code null} si no lo manda
 * @param carreraPropia la única carrera que puede tocar; vacío, cualquiera (D-64)
 * @param quien el usuario que asigna
 */
public record SolicitudDeActividad(
        String periodo,
        String carrera,
        String docente,
        String actividad,
        BigDecimal horas,
        String proyecto,
        Optional<String> carreraPropia,
        String quien) {

    /** Exige los datos. */
    public SolicitudDeActividad {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(docente, "docente");
        Objects.requireNonNull(actividad, "actividad");
        Objects.requireNonNull(horas, "horas");
        Objects.requireNonNull(carreraPropia, "carreraPropia");
        Objects.requireNonNull(quien, "quien");
    }
}
