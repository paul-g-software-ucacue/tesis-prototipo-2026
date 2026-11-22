package ec.mdd.reporting.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * Una descarga de un reporte, que queda en la bitácora (aclaración del 2026-11-21, D-115, D-117).
 * No lleva nombres ni horas: en el individual, la identificación del docente es el identificador
 * del objeto; el general es de todo el periodo y no lleva ninguna, porque no es de un docente.
 *
 * @param tipo individual o general
 * @param usuario quién descargó
 * @param momento cuándo
 * @param periodo el periodo del reporte
 * @param version la versión publicada de la que salió
 * @param identificacion el docente, en el individual; {@code null} en el general
 * @param formato PDF o CSV
 */
public record DescargaDeReporte(
        TipoDeReporte tipo,
        String usuario,
        Instant momento,
        String periodo,
        int version,
        String identificacion,
        Formato formato) {

    /**
     * Exige los datos, y la identificación solo en el individual.
     *
     * @throws IllegalArgumentException si un individual no tiene docente o un general lo tiene
     */
    public DescargaDeReporte {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(formato, "formato");
        if ((tipo == TipoDeReporte.INDIVIDUAL) != (identificacion != null)) {
            throw new IllegalArgumentException(
                    "La identificación va solo en la descarga del reporte individual.");
        }
    }

    /**
     * @return la descarga del reporte individual de un docente
     */
    public static DescargaDeReporte individual(
            String usuario,
            Instant momento,
            String periodo,
            int version,
            String identificacion,
            Formato formato) {
        Objects.requireNonNull(identificacion, "identificacion");
        return new DescargaDeReporte(
                TipoDeReporte.INDIVIDUAL,
                usuario,
                momento,
                periodo,
                version,
                identificacion,
                formato);
    }

    /**
     * @return la descarga del reporte general del periodo
     */
    public static DescargaDeReporte general(
            String usuario, Instant momento, String periodo, int version, Formato formato) {
        return new DescargaDeReporte(
                TipoDeReporte.GENERAL, usuario, momento, periodo, version, null, formato);
    }

    /** El nombre del archivo, sin espacios: el tipo, el periodo y, en el individual, el docente. */
    public String nombreDelArchivo() {
        return tipo.prefijo()
                + "-"
                + periodo
                + (identificacion == null ? "" : "-" + identificacion)
                + "."
                + formato.extension();
    }
}
