package ec.mdd.cargas.adaptadores.entrada;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Escribe los CSV de las plantillas y los diccionarios (research §6): UTF-8 con BOM, para que una
 * hoja de cálculo lea bien las tildes; coma como separador y CRLF al final de cada línea (RFC
 * 4180); comillas dobles si el valor tiene coma, comillas o salto de línea; y un apóstrofo delante
 * de un valor que empiece como fórmula (principio IX), aunque el diccionario ya los prohíbe.
 */
final class EscritorCsv {

    private static final String BOM = "\uFEFF";

    private static final String FIN_DE_LINEA = "\r\n";

    private EscritorCsv() {}

    /**
     * El CSV con esas filas.
     *
     * @param filas las filas, cada una con sus valores; una lista vacía es una fila vacía
     * @return el texto, que se envía en UTF-8
     */
    static String escribir(List<List<String>> filas) {
        StringBuilder csv = new StringBuilder(BOM);
        for (List<String> fila : filas) {
            csv.append(fila.stream().map(EscritorCsv::valor).collect(Collectors.joining(",")))
                    .append(FIN_DE_LINEA);
        }
        return csv.toString();
    }

    private static String valor(String valor) {
        String texto = valor == null ? "" : valor;
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        if (texto.contains(",")
                || texto.contains("\"")
                || texto.contains("\n")
                || texto.contains("\r")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}
