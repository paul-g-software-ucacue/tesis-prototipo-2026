package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Un PDF se comprueba por su contenido, no por su nombre (FR-015). */
class ArchivoPdfTest {

    private static final int DIEZ_MB = 10 * 1024 * 1024;
    private static final byte[] PDF =
            "%PDF-1.7\ncontenido\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    @Test
    void dadoUnPdf_cuandoSeValida_entoncesConservaSuContenidoYSuHuella() {
        ArchivoPdf archivo = ArchivoPdf.de("informe.pdf", PDF);

        assertThat(archivo.contenido()).isEqualTo(PDF);
        assertThat(archivo.nombre()).isEqualTo("informe.pdf");
        assertThat(archivo.huella()).isEqualTo(sha256(PDF));
    }

    @Test
    void dadoUnPdfVacioDeBytes_cuandoSeValida_entoncesLoRechaza() {
        assertThatThrownBy(() -> ArchivoPdf.de("informe.pdf", new byte[0]))
                .isInstanceOf(InformeNoEsPdf.class);
    }

    @Test
    void dadoUnTextoLlamadoPdf_cuandoSeValida_entoncesLoRechaza() {
        assertThatThrownBy(
                        () ->
                                ArchivoPdf.de(
                                        "informe.pdf",
                                        "esto no es un PDF".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InformeNoEsPdf.class);
    }

    @Test
    void dadaUnaImagenLlamadaPdf_cuandoSeValida_entoncesLaRechaza() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13};

        assertThatThrownBy(() -> ArchivoPdf.de("informe.pdf", png))
                .isInstanceOf(InformeNoEsPdf.class);
    }

    @Test
    void dadoUnPdfCortado_cuandoSeValida_entoncesLoRechaza() {
        byte[] cortado = "%PDF-1.7\nsin el final".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> ArchivoPdf.de("informe.pdf", cortado))
                .isInstanceOf(InformeNoEsPdf.class);
    }

    @Test
    void dadosDiezMbJustos_cuandoSeValida_entoncesPasa() {
        assertThat(ArchivoPdf.de("informe.pdf", pdfDe(DIEZ_MB)).contenido()).hasSize(DIEZ_MB);
    }

    @Test
    void dadoUnByteMasDeDiezMb_cuandoSeValida_entoncesLoRechaza() {
        assertThatThrownBy(() -> ArchivoPdf.de("informe.pdf", pdfDe(DIEZ_MB + 1)))
                .isInstanceOf(InformeDemasiadoGrande.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../etc/informe.pdf", "C:\\docs\\informe.pdf", "/tmp/informe.pdf"})
    void conUnaRutaEnElNombre_cuandoSeValida_entoncesSoloQuedaElNombre(String nombre) {
        assertThat(ArchivoPdf.de(nombre, PDF).nombre()).isEqualTo("informe.pdf");
    }

    @Test
    void conCaracteresDeControl_cuandoSeValida_entoncesLosQuita() {
        assertThat(ArchivoPdf.de("info\r\nrme\t.pdf", PDF).nombre()).isEqualTo("informe.pdf");
    }

    @Test
    void conUnNombreLargo_cuandoSeValida_entoncesLoCortaA150() {
        assertThat(ArchivoPdf.de("x".repeat(200) + ".pdf", PDF).nombre()).hasSize(150);
    }

    @Test
    void sinNombre_cuandoSeValida_entoncesUsaUnoFijo() {
        assertThat(ArchivoPdf.de(null, PDF).nombre()).isEqualTo("informe.pdf");
        assertThat(ArchivoPdf.de("  ", PDF).nombre()).isEqualTo("informe.pdf");
    }

    private static byte[] pdfDe(int largo) {
        byte[] bytes = new byte[largo];
        Arrays.fill(bytes, (byte) ' ');
        byte[] inicio = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
        byte[] fin = "%%EOF".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(inicio, 0, bytes, 0, inicio.length);
        System.arraycopy(fin, 0, bytes, largo - fin.length, fin.length);
        return bytes;
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }
}
