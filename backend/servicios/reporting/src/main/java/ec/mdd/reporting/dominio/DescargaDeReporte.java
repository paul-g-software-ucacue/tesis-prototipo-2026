package ec.mdd.reporting.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * Una descarga de un reporte individual, que queda en la bitácora (aclaración del 2026-11-21,
 * D-115). No lleva el nombre del docente ni sus horas: la identificación es el identificador del
 * objeto.
 *
 * @param usuario quién descargó
 * @param momento cuándo
 * @param periodo el periodo del reporte
 * @param version la versión publicada de la que salió
 * @param identificacion el docente
 * @param formato PDF o CSV
 */
public record DescargaDeReporte(
        String usuario,
        Instant momento,
        String periodo,
        int version,
        String identificacion,
        Formato formato) {

    /** Exige los datos. */
    public DescargaDeReporte {
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(identificacion, "identificacion");
        Objects.requireNonNull(formato, "formato");
    }
}
