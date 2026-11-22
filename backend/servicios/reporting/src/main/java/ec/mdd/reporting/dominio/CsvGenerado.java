package ec.mdd.reporting.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * Un CSV para SIGA entregado, para la bitácora (FR-021, D-119). No lleva nombres, identificaciones
 * ni horas (principio IX). No es la {@code ExportacionSiga} con la suma de verificación, que es de
 * F19-H02.
 *
 * @param usuario quién lo generó
 * @param momento cuándo
 * @param periodo el código del periodo
 * @param version la versión publicada de la que salió
 * @param layout la versión del layout de exportación con que salió; 0 si fue el por defecto
 */
public record CsvGenerado(
        String usuario, Instant momento, String periodo, int version, int layout) {

    /** Exige los datos. */
    public CsvGenerado {
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(periodo, "periodo");
        if (version < 1 || layout < 0) {
            throw new IllegalArgumentException("versión desde 1 y layout desde 0");
        }
    }
}
