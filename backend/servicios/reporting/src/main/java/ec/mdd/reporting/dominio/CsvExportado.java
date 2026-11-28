package ec.mdd.reporting.dominio;

import java.util.Objects;

/**
 * Lo que entrega el caso de uso del CSV para SIGA (F19-H02, D-120): el archivo y su registro, ya
 * guardado.
 *
 * @param archivo el CSV para SIGA
 * @param exportacion su registro, con la suma de verificación
 */
public record CsvExportado(ArchivoSiga archivo, ExportacionSiga exportacion) {

    /** Exige los dos. */
    public CsvExportado {
        Objects.requireNonNull(archivo, "archivo");
        Objects.requireNonNull(exportacion, "exportacion");
    }
}
