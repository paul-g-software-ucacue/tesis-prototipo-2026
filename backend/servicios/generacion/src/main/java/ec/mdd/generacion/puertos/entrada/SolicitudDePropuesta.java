package ec.mdd.generacion.puertos.entrada;

import java.util.Objects;
import java.util.Optional;

/**
 * Lo que pide quien propone.
 *
 * @param periodo el código del periodo
 * @param asignatura el código de la asignatura
 * @param paralelo el paralelo
 * @param docente la identificación del docente
 * @param texto el texto de la justificación; {@code null} si no lo manda
 * @param carrera la única carrera que puede tocar; vacío, cualquiera (D-64)
 * @param quien el usuario que propone
 */
public record SolicitudDePropuesta(
        String periodo,
        String asignatura,
        String paralelo,
        String docente,
        String texto,
        Optional<String> carrera,
        String quien) {

    /** Exige los datos. */
    public SolicitudDePropuesta {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(asignatura, "asignatura");
        Objects.requireNonNull(paralelo, "paralelo");
        Objects.requireNonNull(docente, "docente");
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(quien, "quien");
    }
}
