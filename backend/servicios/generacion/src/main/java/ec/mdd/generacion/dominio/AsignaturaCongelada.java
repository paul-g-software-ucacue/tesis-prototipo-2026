package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una asignatura-paralelo de un docente tal como quedó al congelar la versión (F20-H01, D-115): las
 * horas de clase son las de la oferta en ese momento, no las de hoy.
 *
 * @param codigo el código de la asignatura
 * @param nombre su nombre
 * @param paralelo el paralelo
 * @param carrera el código de su carrera
 * @param nombreCarrera el nombre de la carrera; el código si Académico no lo tenía
 * @param horas sus horas semanales de clase
 */
public record AsignaturaCongelada(
        String codigo,
        String nombre,
        String paralelo,
        String carrera,
        String nombreCarrera,
        BigDecimal horas) {

    /** Exige los datos y horas no negativas. */
    public AsignaturaCongelada {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(paralelo, "paralelo");
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(nombreCarrera, "nombreCarrera");
        Objects.requireNonNull(horas, "horas");
        if (horas.signum() < 0) {
            throw new IllegalArgumentException("horas negativas");
        }
    }
}
