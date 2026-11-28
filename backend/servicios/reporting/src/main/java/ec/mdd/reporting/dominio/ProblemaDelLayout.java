package ec.mdd.reporting.dominio;

import java.util.Objects;

/**
 * Un problema de un archivo de mapeo: dónde está y qué hacer (FR-015, principio XIII).
 *
 * @param columna {@code columnas[3]}, {@code columnas[3] (HORAS)} o {@code archivo}
 * @param regla qué falla y cómo corregirlo, en español
 */
public record ProblemaDelLayout(String columna, String regla) {

    /** Exige los dos. */
    public ProblemaDelLayout {
        Objects.requireNonNull(columna, "columna");
        Objects.requireNonNull(regla, "regla");
    }
}
