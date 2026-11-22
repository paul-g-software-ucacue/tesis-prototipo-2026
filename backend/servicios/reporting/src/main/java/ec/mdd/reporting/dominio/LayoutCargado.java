package ec.mdd.reporting.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * Una versión nueva del layout de exportación, para la bitácora (FR-017, D-119).
 *
 * @param usuario quién la cargó
 * @param momento cuándo
 * @param layout su número
 * @param columnas cuántas columnas tiene
 * @param sha256 el SHA-256 del archivo
 */
public record LayoutCargado(
        String usuario, Instant momento, int layout, int columnas, String sha256) {

    /** Exige los datos. */
    public LayoutCargado {
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(sha256, "sha256");
        if (layout < 1 || columnas < 1) {
            throw new IllegalArgumentException("una versión cargada empieza en 1 y tiene columnas");
        }
    }
}
