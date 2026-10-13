package ec.mdd.firma.spike;

import static org.assertj.core.api.Assertions.assertThat;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;

/**
 * Genera el PDF/A-2b de prueba del spike B00-S01 con OpenHTMLtoPDF y comprueba con veraPDF que
 * cumple PDF/A-2b antes de firmarlo (FR-003, research §4).
 *
 * <p>Deja una copia en {@code backend/servicios/firma/build/spike-firma/pdfa-prueba.pdf} para que
 * el autor la firme con FirmaEC. La generación agrega fechas, así que no es reproducible byte a
 * byte: la versión que se firmó se guarda aparte en {@code ca-prueba/pdfa-prueba.pdf}.
 */
class PdfaDePruebaTest {

    /** Carpeta de la copia para firmar, relativa al directorio del servicio. */
    static final Path SALIDA = Path.of("build", "spike-firma", "pdfa-prueba.pdf");

    @Test
    void generaUnPdfaDosBConforme() throws Exception {
        byte[] pdf = generar();

        List<String> incumplidas = reglasIncumplidas(pdf);

        assertThat(incumplidas).as("reglas de PDF/A-2b que no cumple").isEmpty();
        Files.createDirectories(SALIDA.getParent());
        Files.write(SALIDA, pdf);
    }

    @Test
    void unPdfComunNoPasaPorPdfa() throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PDDocument documento = new PDDocument()) {
            documento.addPage(new PDPage());
            documento.save(salida);
        }

        assertThat(reglasIncumplidas(salida.toByteArray()))
                .as("control: veraPDF rechaza un PDF sin PDF/A")
                .isNotEmpty();
    }

    /**
     * Genera el PDF/A-2b desde la plantilla ficticia, con DejaVu Sans incrustada y el perfil sRGB.
     *
     * @return los bytes del PDF generado
     * @throws IOException si no se puede leer la plantilla o la fuente
     */
    static byte[] generar() throws IOException {
        String html;
        try (InputStream plantilla = recurso("/spike/plantilla-pdfa.html")) {
            html = new String(plantilla.readAllBytes(), StandardCharsets.UTF_8);
        }
        byte[] fuente;
        try (InputStream entrada = recurso("/fuentes/DejaVuSans.ttf")) {
            fuente = entrada.readAllBytes();
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PdfRendererBuilder constructor = new PdfRendererBuilder();
        constructor.useFastMode();
        constructor.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_B);
        constructor.useColorProfile(ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData());
        constructor.useFont(() -> new ByteArrayInputStream(fuente), "DejaVu Sans");
        constructor.withHtmlContent(html, null);
        constructor.toStream(salida);
        constructor.run();
        return salida.toByteArray();
    }

    /**
     * Valida el PDF con veraPDF contra PDF/A-2b.
     *
     * @param pdf los bytes del documento
     * @return las reglas incumplidas, con su mensaje; vacía si el documento es conforme
     * @throws Exception si veraPDF no puede leer el documento
     */
    static List<String> reglasIncumplidas(byte[] pdf) throws Exception {
        VeraGreenfieldFoundryProvider.initialise();
        try (PDFAParser lector =
                        Foundries.defaultInstance()
                                .createParser(new ByteArrayInputStream(pdf), PDFAFlavour.PDFA_2_B);
                PDFAValidator validador =
                        Foundries.defaultInstance().createValidator(PDFAFlavour.PDFA_2_B, false)) {
            ValidationResult resultado = validador.validate(lector);
            if (resultado.isCompliant()) {
                return List.of();
            }
            return resultado.getTestAssertions().stream()
                    .filter(afirmacion -> afirmacion.getStatus() == TestAssertion.Status.FAILED)
                    .map(afirmacion -> afirmacion.getRuleId() + ": " + afirmacion.getMessage())
                    .distinct()
                    .toList();
        }
    }

    private static InputStream recurso(String ruta) {
        return Objects.requireNonNull(
                PdfaDePruebaTest.class.getResourceAsStream(ruta), "falta el recurso " + ruta);
    }
}
