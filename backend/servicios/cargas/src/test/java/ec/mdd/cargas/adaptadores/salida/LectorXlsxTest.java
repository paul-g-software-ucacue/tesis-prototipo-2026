package ec.mdd.cargas.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.cargas.dominio.TablaDeDatos;
import ec.mdd.cargas.puertos.salida.ArchivoIlegible;
import ec.mdd.cargas.puertos.salida.FormatoNoAdmitido;
import ec.mdd.cargas.puertos.salida.VariasHojas;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
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

    @Test
    void dadoUnXlsxConDosHojas_cuandoSeLee_entoncesVariasHojas() throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            libro.createSheet("Datos").createRow(0).createCell(0).setCellValue("a");
            libro.createSheet("Otra");

            byte[] contenido = LibroDePrueba.bytes(libro);
            assertThatThrownBy(() -> lector.leer(contenido)).isInstanceOf(VariasHojas.class);
        }
    }

    @Test
    void dadoUnValorFueraDeLasColumnas_cuandoSeLee_entoncesIlegibleConSuFila() throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Datos");
            hoja.createRow(0).createCell(0).setCellValue("a");
            hoja.getRow(0).createCell(1).setCellValue("b");
            hoja.createRow(1).createCell(0).setCellValue("1");
            hoja.getRow(1).createCell(2).setCellValue("de más");

            byte[] contenido = LibroDePrueba.bytes(libro);
            assertThatThrownBy(() -> lector.leer(contenido))
                    .isInstanceOf(ArchivoIlegible.class)
                    .hasMessage(
                            "La fila 2 tiene valores fuera de las columnas de los encabezados.");
        }
    }

    @Test
    void dadoUnXlsxQueEsOtraCosa_cuandoSeLee_entoncesFormatoNoAdmitido() throws Exception {
        byte[] texto = "codigo,nombre\n".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream xls = new ByteArrayOutputStream();
        try (POIFSFileSystem ole2 = new POIFSFileSystem()) {
            ole2.createDocument(new java.io.ByteArrayInputStream(new byte[8]), "Workbook");
            ole2.writeFilesystem(xls);
        }

        assertThatThrownBy(() -> lector.leer(texto)).isInstanceOf(FormatoNoAdmitido.class);
        assertThatThrownBy(() -> lector.leer(xls.toByteArray()))
                .isInstanceOf(FormatoNoAdmitido.class);
    }

    @Test
    void dadoUnXlsxCifradoODanado_cuandoSeLee_entoncesIlegible() throws Exception {
        byte[] xlsx = LibroDePrueba.deTexto(List.of("a"), List.of(List.of("1")));
        ByteArrayOutputStream cifrado = new ByteArrayOutputStream();
        try (POIFSFileSystem ole2 = new POIFSFileSystem()) {
            EncryptionInfo info = new EncryptionInfo(EncryptionMode.agile);
            Encryptor cifrador = info.getEncryptor();
            cifrador.confirmPassword("clave-de-prueba");
            try (OutputStream salida = cifrador.getDataStream(ole2)) {
                salida.write(xlsx);
            }
            ole2.writeFilesystem(cifrado);
        }
        byte[] danado = java.util.Arrays.copyOf(xlsx, xlsx.length / 2);

        assertThatThrownBy(() -> lector.leer(cifrado.toByteArray()))
                .isInstanceOf(ArchivoIlegible.class)
                .hasMessageContaining("contraseña");
        assertThatThrownBy(() -> lector.leer(danado))
                .isInstanceOf(ArchivoIlegible.class)
                .hasMessageContaining("dañado");
    }
}
