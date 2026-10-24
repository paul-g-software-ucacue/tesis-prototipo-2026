package ec.mdd.cargas.dominio;

import java.util.List;

/**
 * Lo que entrega una fuente de datos de una carga (RF-57): los encabezados y las filas, como textos
 * sin convertir. Un archivo CSV o XLSX es hoy la única fuente; otra, como Moodle o SIGA, entregaría
 * la misma tabla sin cambiar las validaciones.
 *
 * @param encabezados los nombres de las columnas, como vienen
 * @param filas las filas de datos, cada una con sus valores
 */
public record TablaDeDatos(List<String> encabezados, List<List<String>> filas) {

    /** Copia las listas. */
    public TablaDeDatos {
        encabezados = List.copyOf(encabezados);
        filas = filas.stream().map(List::copyOf).toList();
    }

    /**
     * Cuántos registros tiene: las filas sin las vacías del final. Una fila vacía tiene todos sus
     * valores en blanco; si está entre registros, sí cuenta, y su contenido lo revisa F01-H03.
     */
    public int registros() {
        int ultima = filas.size();
        while (ultima > 0 && vacia(filas.get(ultima - 1))) {
            ultima--;
        }
        return ultima;
    }

    private static boolean vacia(List<String> fila) {
        return fila.stream().allMatch(String::isBlank);
    }
}
