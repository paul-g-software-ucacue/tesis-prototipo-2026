package ec.mdd.generacion.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * Cuándo y quién consolidó una versión. Solo se guarda esto: el consolidado se calcula al abrirlo.
 *
 * @param por quién consolidó
 * @param en cuándo
 */
public record Consolidacion(String por, Instant en) {

    /** Exige los datos. */
    public Consolidacion {
        Objects.requireNonNull(por, "por");
        Objects.requireNonNull(en, "en");
    }
}
