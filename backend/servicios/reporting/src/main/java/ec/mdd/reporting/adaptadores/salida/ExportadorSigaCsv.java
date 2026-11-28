package ec.mdd.reporting.adaptadores.salida;

import ec.mdd.reporting.dominio.ArchivoSiga;
import ec.mdd.reporting.dominio.CeldaCsv;
import ec.mdd.reporting.dominio.ColumnaDelLayout;
import ec.mdd.reporting.dominio.FilaSiga;
import ec.mdd.reporting.dominio.FilasParaSiga;
import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.dominio.ValorDeColumna;
import ec.mdd.reporting.puertos.salida.ExportadorSiga;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * El CSV para SIGA del prototipo (research §4, D-119): RFC 4180 con coma y CRLF, UTF-8 sin BOM, y
 * las convenciones de carga masiva de SENESCYT (ADR-002). El layout decide las columnas; el formato
 * es fijo.
 *
 * <p>Sin BOM, porque la RFC 4180 no lo prevé y un importador puede leerlo como parte del primer
 * encabezado. Sin el momento de generación, para que dos archivos de la misma versión y el mismo
 * layout sean idénticos y su suma de verificación sea estable (FR-007, F19-H02).
 */
public final class ExportadorSigaCsv implements ExportadorSiga {

    private static final String SEPARADOR = ",";
    private static final String FIN_DE_LINEA = "\r\n";
    private static final String NO_APLICA = "NA";

    @Override
    public ArchivoSiga exportar(FilasParaSiga filas, LayoutExportacion layout) {
        StringBuilder csv = new StringBuilder();
        linea(csv, layout.encabezado());
        for (FilaSiga fila : filas.filas()) {
            List<String> celdas = new ArrayList<>();
            for (ColumnaDelLayout columna : layout.columnas()) {
                celdas.add(texto(columna.valor(fila)));
            }
            linea(csv, celdas);
        }
        return new ArchivoSiga(
                csv.toString().getBytes(StandardCharsets.UTF_8),
                ArchivoSiga.nombreDe(filas.publicacion()));
    }

    private static String texto(ValorDeColumna valor) {
        return switch (valor) {
            case ValorDeColumna.Texto texto -> texto.valor().toUpperCase(Locale.ROOT);
            case ValorDeColumna.Entero entero -> String.valueOf(entero.valor());
            case ValorDeColumna.Horas horas ->
                    horas.valor().setScale(2, RoundingMode.UNNECESSARY).toPlainString();
            case ValorDeColumna.Fecha fecha ->
                    DateTimeFormatter.ISO_LOCAL_DATE.format(fecha.valor());
            case ValorDeColumna.NoAplica noAplica -> NO_APLICA;
        };
    }

    private static void linea(StringBuilder csv, List<String> celdas) {
        List<String> escritas = new ArrayList<>();
        for (String celda : celdas) {
            escritas.add(campo(CeldaCsv.neutralizada(celda)));
        }
        csv.append(String.join(SEPARADOR, escritas)).append(FIN_DE_LINEA);
    }

    // RFC 4180: comillas solo si el valor tiene coma, comillas o un salto de línea.
    private static String campo(String valor) {
        boolean conComillas =
                valor.contains(SEPARADOR)
                        || valor.contains("\"")
                        || valor.contains("\r")
                        || valor.contains("\n");
        return conComillas ? "\"" + valor.replace("\"", "\"\"") + "\"" : valor;
    }
}
