package ec.mdd.validacionocs.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** El PDF de la resolución, comprobado como el informe sustentado y sin mirar sus firmas (D-19). */
class ArchivoDeResolucionTest {

    private static byte[] pdf(int tamano) {
        byte[] bytes = new byte[tamano];
        Arrays.fill(bytes, (byte) ' ');
        byte[] inicio = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
        byte[] fin = "%%EOF\n".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(inicio, 0, bytes, 0, inicio.length);
        System.arraycopy(fin, 0, bytes, tamano - fin.length, fin.length);
        return bytes;
    }

    @Test
    void dadoUnPdf_cuandoSeComprueba_entoncesTraeSuHuellaSuTamanoYElNombreSinRuta()
            throws Exception {
        byte[] contenido = pdf(4096);

        ArchivoDeResolucion archivo =
                ArchivoDeResolucion.de("C:\\Documentos\\resolucion 123.pdf", contenido);

        assertThat(archivo.sha256())
                .isEqualTo(
                        HexFormat.of()
                                .formatHex(MessageDigest.getInstance("SHA-256").digest(contenido)));
        assertThat(archivo.tamano()).isEqualTo(4096);
        assertThat(archivo.nombre()).isEqualTo("resolucion 123.pdf");
        assertThat(archivo.contenido()).isEqualTo(contenido);
        assertThat(ArchivoDeResolucion.de("/tmp/a/b.pdf", contenido).nombre()).isEqualTo("b.pdf");
        assertThat(ArchivoDeResolucion.de(null, contenido).nombre()).isEqualTo("resolucion.pdf");
    }

    @Test
    void dadoUnArchivoQueNoEsPdf_cuandoSeComprueba_entoncesSeRechaza() {
        byte[] sinFin = pdf(100);
        sinFin[sinFin.length - 2] = 'X';
        byte[] sinInicio = pdf(100);
        sinInicio[0] = 'X';

        assertThatThrownBy(() -> ArchivoDeResolucion.de("r.pdf", new byte[0]))
                .isInstanceOf(ResolucionNoEsPdf.class);
        assertThatThrownBy(() -> ArchivoDeResolucion.de("r.pdf", sinInicio))
                .isInstanceOf(ResolucionNoEsPdf.class);
        assertThatThrownBy(() -> ArchivoDeResolucion.de("r.pdf", sinFin))
                .isInstanceOf(ResolucionNoEsPdf.class);
    }

    @Test
    void dadoUnPdfDe10MegasJustos_cuandoSeComprueba_entoncesSeAdmiteYConUnByteMasNo() {
        assertThat(ArchivoDeResolucion.de("r.pdf", pdf(10 * 1024 * 1024)).tamano())
                .isEqualTo(10 * 1024 * 1024);
        assertThatThrownBy(() -> ArchivoDeResolucion.de("r.pdf", pdf(10 * 1024 * 1024 + 1)))
                .isInstanceOf(ResolucionDemasiadoGrande.class);
    }
}
