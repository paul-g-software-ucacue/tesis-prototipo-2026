package ec.mdd.normativa.dominio;

import java.time.LocalDate;
import java.util.Objects;

/**
 * La cabecera de una versión de normativa: lo que la distingue de la versión de la que se copia
 * (F04-H02).
 *
 * @param codigo código de su origen, único entre las versiones, por ejemplo {@code
 *     SENESCYT-2025-01}
 * @param origen de dónde sale, por ejemplo «Reforma de la política 2025»
 * @param vigenteDesde desde cuándo rige la norma
 */
public record DatosDeVersion(String codigo, String origen, LocalDate vigenteDesde) {

    /** Exige código, origen y vigencia. */
    public DatosDeVersion {
        if (codigo == null || codigo.isBlank() || origen == null || origen.isBlank()) {
            throw new IllegalArgumentException("La versión necesita código y origen");
        }
        Objects.requireNonNull(vigenteDesde, "vigenteDesde");
    }
}
