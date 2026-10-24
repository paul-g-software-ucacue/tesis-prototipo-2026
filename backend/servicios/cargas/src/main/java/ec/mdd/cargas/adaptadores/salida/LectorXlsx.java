package ec.mdd.cargas.adaptadores.salida;

import ec.mdd.cargas.dominio.TablaDeDatos;
import ec.mdd.cargas.puertos.salida.ArchivoIlegible;
import ec.mdd.cargas.puertos.salida.VariasHojas;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Lee el XLSX de una carga con Apache POI (research §5): una sola hoja, los encabezados en la
 * primera fila y cada celda como el texto que muestra, para que una cédula conserve su 0 inicial y
 * una fecha salga como se ve. Las fórmulas dan su último valor calculado; nunca se evalúan. Las
 * celdas vacías al final de una fila son valores en blanco; un valor fuera de las columnas de los
 * encabezados hace que el archivo no se pueda leer.
 */
final class LectorXlsx {

    /**
     * Lee el libro.
     *
     * @param contenido los bytes del XLSX
     * @return sus encabezados y sus filas; vacía si la hoja no tiene nada
     * @throws VariasHojas si tiene más de una hoja
     * @throws ArchivoIlegible si está dañado o tiene valores fuera de las columnas
     */
    TablaDeDatos leer(byte[] contenido) {
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            if (libro.getNumberOfSheets() > 1) {
                throw new VariasHojas();
            }
            return tabla(libro.getSheetAt(0));
        } catch (IOException | RuntimeException e) {
            if (e instanceof VariasHojas || e instanceof ArchivoIlegible) {
                throw (RuntimeException) e;
            }
            throw new ArchivoIlegible("El archivo XLSX está dañado o no se puede abrir.");
        }
    }

    private static TablaDeDatos tabla(Sheet hoja) {
        DataFormatter formato = new DataFormatter(Locale.ROOT);
        formato.setUseCachedValuesForFormulaCells(true);
        Row primera = hoja.getRow(0);
        if (primera == null) {
            return new TablaDeDatos(List.of(), List.of());
        }
        List<String> encabezados = valores(primera, formato, primera.getLastCellNum());
        while (!encabezados.isEmpty() && encabezados.getLast().isBlank()) {
            encabezados.removeLast();
        }
        int ancho = encabezados.size();
        List<List<String>> filas = new ArrayList<>();
        for (int i = 1; i <= hoja.getLastRowNum(); i++) {
            Row fila = hoja.getRow(i);
            if (fila == null) {
                filas.add(Collections.nCopies(ancho, ""));
                continue;
            }
            List<String> todos = valores(fila, formato, Math.max(ancho, fila.getLastCellNum()));
            if (todos.subList(ancho, todos.size()).stream().anyMatch(valor -> !valor.isBlank())) {
                throw new ArchivoIlegible(
                        "La fila "
                                + (i + 1)
                                + " tiene valores fuera de las columnas de los encabezados.");
            }
            filas.add(todos.subList(0, ancho));
        }
        return new TablaDeDatos(encabezados, filas);
    }

    private static List<String> valores(Row fila, DataFormatter formato, int cuantos) {
        List<String> valores = new ArrayList<>();
        for (int i = 0; i < cuantos; i++) {
            Cell celda = fila.getCell(i);
            valores.add(celda == null ? "" : formato.formatCellValue(celda));
        }
        return valores;
    }
}
