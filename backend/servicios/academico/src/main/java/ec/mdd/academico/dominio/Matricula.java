package ec.mdd.academico.dominio;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Los matriculados de una carrera en un periodo (glosario «Matriculados», carga 10): un total, sin
 * datos personales (RNF-SEG-06).
 *
 * @param periodo el código del periodo
 * @param carrera el código de la carrera
 * @param total cuántos estudiantes, de 0 a 99 999 (el largo 5 del diccionario)
 * @param fechaCorte a qué fecha
 * @param fuente de dónde sale, como SIGA; {@code null} si no se dice
 */
public record Matricula(
        String periodo, String carrera, int total, LocalDate fechaCorte, String fuente) {

    /** El total más alto: 5 dígitos, el largo de la columna. */
    public static final int TOTAL_MAXIMO = 99_999;

    /** El largo máximo de la fuente. */
    public static final int FUENTE_MAXIMA = 60;

    /** Valida los datos. */
    public Matricula {
        if (periodo == null || periodo.isBlank()) {
            throw new DatoInvalido("periodo", "Elige el periodo de los matriculados");
        }
        if (carrera == null || carrera.isBlank()) {
            throw new DatoInvalido("carrera", "Elige la carrera de los matriculados");
        }
        if (total < 0 || total > TOTAL_MAXIMO) {
            throw new DatoInvalido("total", "El total va de 0 a 99 999 estudiantes");
        }
        Objects.requireNonNull(fechaCorte, "fechaCorte");
        if (fuente != null && (fuente.isBlank() || fuente.length() > FUENTE_MAXIMA)) {
            throw new DatoInvalido("fuente", "La fuente tiene hasta 60 caracteres");
        }
    }
}
