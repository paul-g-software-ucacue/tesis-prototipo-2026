package ec.mdd.cargas.adaptadores.salida;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Arma XLSX de prueba con POI: una hoja con los encabezados y las filas como texto. */
public final class LibroDePrueba {

    private LibroDePrueba() {}

    /** Un XLSX de una hoja con esos encabezados y esas filas, cada celda como texto. */
    public static byte[] deTexto(List<String> encabezados, List<List<String>> filas) {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Datos");
            escribir(hoja.createRow(0), encabezados);
            for (int i = 0; i < filas.size(); i++) {
                escribir(hoja.createRow(i + 1), filas.get(i));
            }
            return bytes(libro);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Los bytes del libro. */
    public static byte[] bytes(XSSFWorkbook libro) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        libro.write(salida);
        return salida.toByteArray();
    }

    private static void escribir(Row fila, List<String> valores) {
        for (int i = 0; i < valores.size(); i++) {
            if (!valores.get(i).isEmpty()) {
                fila.createCell(i).setCellValue(valores.get(i));
            }
        }
    }
}
