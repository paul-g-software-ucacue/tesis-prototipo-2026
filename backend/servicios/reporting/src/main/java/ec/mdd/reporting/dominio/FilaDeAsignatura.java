package ec.mdd.reporting.dominio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Una asignatura-paralelo que dicta el docente en la versión, con sus horas de clase.
 *
 * @param carrera el nombre de la carrera
 * @param codigo el código de la asignatura
 * @param nombre su nombre
 * @param paralelo el paralelo
 * @param horas sus horas semanales de clase
 * @param codigoDeCarrera el código de la carrera, para el CSV para SIGA (D-119); {@code null} en
 *     los reportes, que muestran el nombre
 */
public record FilaDeAsignatura(
        String carrera,
        String codigo,
        String nombre,
        String paralelo,
        BigDecimal horas,
        String codigoDeCarrera) {

    /** Exige los datos y horas válidas. */
    public FilaDeAsignatura {
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(paralelo, "paralelo");
        horas = Horas.validas(horas);
    }

    /** Una asignatura de los reportes, sin el código de la carrera. */
    public FilaDeAsignatura(
            String carrera, String codigo, String nombre, String paralelo, BigDecimal horas) {
        this(carrera, codigo, nombre, paralelo, horas, null);
    }

    /** El código de la carrera; vacío si no se leyó. */
    public Optional<String> codigoCarrera() {
        return Optional.ofNullable(codigoDeCarrera);
    }
}
