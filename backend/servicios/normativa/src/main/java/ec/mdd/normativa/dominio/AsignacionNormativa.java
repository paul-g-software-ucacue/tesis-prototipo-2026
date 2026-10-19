package ec.mdd.normativa.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Asignación normativa: el periodo académico, nombrado por su código, al que se asocia una versión
 * de normativa (glosario §7.3, modelo de dominio §5.1). Un periodo tiene una sola versión; desde
 * que una versión tiene un periodo, no cambia. No es una asignación de horas.
 *
 * <p>Normativa no comprueba que el periodo exista: es dato de Académico, que asocia la versión al
 * abrir el periodo (F02-H01, principio V).
 *
 * @param periodo el código del periodo, por ejemplo {@code 2025-1}
 * @param asociadaEn cuándo se asoció
 */
public record AsignacionNormativa(String periodo, Instant asociadaEn) {

    private static final Pattern CODIGO = Pattern.compile("[A-Za-z0-9._-]{1,20}");

    /** Comprueba el código del periodo. */
    public AsignacionNormativa {
        if (periodo == null || !CODIGO.matcher(periodo).matches()) {
            throw new IllegalArgumentException("Código de periodo mal formado: " + periodo);
        }
        Objects.requireNonNull(asociadaEn, "asociadaEn");
    }
}
