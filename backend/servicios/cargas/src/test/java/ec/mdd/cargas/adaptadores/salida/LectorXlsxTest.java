package ec.mdd.cargas.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.cargas.dominio.TablaDeDatos;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * El lector de XLSX (research §5): una hoja, el texto que muestra cada celda y el ancho de los
 * encabezados, con los 10 archivos de prueba de la ERS convertidos a XLSX.
 */
class LectorXlsxTest {

    private final LectorXlsx lector = new LectorXlsx();

    @Test
    void dadosLosDiezArchivosDePruebaEnXlsx_cuandoSeLeen_entoncesDanLoMismoQueElCsv() {
        LectorCsv csv = new LectorCsv();
        for (Map.Entry<String, Integer> archivo : DatosDePrueba.REGISTROS.entrySet()) {
            TablaDeDatos delCsv = csv.leer(DatosDePrueba.contenido(archivo.getKey()));
            byte[] xlsx = LibroDePrueba.deTexto(delCsv.encabezados(), delCsv.filas());

            TablaDeDatos delXlsx = lector.leer(xlsx);

            assertThat(delXlsx).as(archivo.getKey()).isEqualTo(delCsv);
            assertThat(delXlsx.registros()).isEqualTo(archivo.getValue());
        }
    }

    @Test
    void dadasCeldasConFormato_cuandoSeLeen_entoncesSaleElTextoQueMuestran() throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Docentes");
            Row encabezados = hoja.createRow(0);
            encabezados.createCell(0).setCellValue("numero_identificacion");
            encabezados.createCell(1).setCellValue("fecha_ingreso");
            encabezados.createCell(2).setCellValue("horas");
            encabezados.createCell(3).setCellValue("texto");
            Row fila = hoja.createRow(1);
            CellStyle diezDigitos = libro.createCellStyle();
            diezDigitos.setDataFormat(libro.createDataFormat().getFormat("0000000000"));
            fila.createCell(0).setCellValue(101010101);
            fila.getCell(0).setCellStyle(diezDigitos);
            CellStyle fecha = libro.createCellStyle();
            fecha.setDataFormat(libro.createDataFormat().getFormat("yyyy-mm-dd"));
            fila.createCell(1).setCellValue(LocalDate.of(2026, 4, 6));
            fila.getCell(1).setCellStyle(fecha);
            fila.createCell(2).setCellValue(2.5);
            fila.createCell(3).setCellValue("0101010101");

            TablaDeDatos tabla = lector.leer(LibroDePrueba.bytes(libro));

            assertThat(tabla.filas())
                    .containsExactly(List.of("0101010101", "2026-04-06", "2.5", "0101010101"));
        }
    }

    @Test
    void dadasFilasCortasYVaciasAlFinal_cuandoSeLeen_entoncesTienenElAnchoDeLosEncabezados() {
        byte[] xlsx =
                LibroDePrueba.deTexto(
                        List.of("a", "b", "c"),
                        List.of(List.of("1", "", ""), List.of("", "", ""), List.of("4", "5", "")));

        TablaDeDatos tabla = lector.leer(xlsx);

        assertThat(tabla.filas())
                .containsExactly(List.of("1", "", ""), List.of("", "", ""), List.of("4", "5", ""));
        assertThat(tabla.registros()).isEqualTo(3);
    }
}
