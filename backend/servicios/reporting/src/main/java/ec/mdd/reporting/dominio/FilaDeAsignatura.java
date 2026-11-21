package ec.mdd.reporting.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una asignatura-paralelo que dicta el docente en la versión, con sus horas de clase.
 *
 * @param carrera el nombre de la carrera
 * @param codigo el código de la asignatura
 * @param nombre su nombre
 * @param paralelo el paralelo
 * @param horas sus horas semanales de clase
 */
public record FilaDeAsignatura(
        String carrera, String codigo, String nombre, String paralelo, BigDecimal horas) {

    /** Exige los datos y horas válidas. */
    public FilaDeAsignatura {
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(paralelo, "paralelo");
        horas = Horas.validas(horas);
    }
}
