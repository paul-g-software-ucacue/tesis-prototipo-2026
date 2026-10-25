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
     * Cuántos registros tiene: las filas que no están vacías. Una fila vacía tiene todos sus
     * valores en blanco; si está entre registros, el informe de la carga la avisa (F01-H03).
     */
    public int registros() {
        return (int) filas.stream().filter(fila -> !vacia(fila)).count();
    }

    private static boolean vacia(List<String> fila) {
        return fila.stream().allMatch(String::isBlank);
    }
}
