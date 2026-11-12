package ec.mdd.firma.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.firma.dominio.ContenidoDelDocumento;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/**
 * El PDF/A-2b del documento firmable (F13-H02, FR-002 a FR-004, SC-001, SC-003), verificado con
 * veraPDF como el spike B00-S01. Sin Docker.
 */
class GeneradorPdfaTest {

    /** Carpeta de la copia para revisar a mano, relativa al directorio del servicio. */
    private static final Path SALIDA = Path.of("build", "documento-firmable");

    private final GeneradorPdfa generador = new GeneradorPdfa();

    @Test
    void dadoUnContenidoPequeno_cuandoSeGenera_entoncesEsPdfaDosBConforme() throws Exception {
        assertThat(
                        VerificadorPdfa.reglasIncumplidas(
                                generador.generar(ContenidosDePrueba.pequeno())))
                .as("reglas de PDF/A-2b que no cumple")
                .isEmpty();
    }

    @Test
    void dadoElContenidoCompleto_cuandoSeGenera_entoncesEsConformeYTardaMenosDe30Segundos()
            throws Exception {
        Instant antes = Instant.now();
        byte[] pdf = generador.generar(ContenidosDePrueba.completo());
        Duration tardo = Duration.between(antes, Instant.now());

        assertThat(tardo).isLessThan(Duration.ofSeconds(30));
        assertThat(VerificadorPdfa.reglasIncumplidas(pdf)).isEmpty();
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(documento.getNumberOfPages()).isGreaterThan(2);
        }
    }

    @Test
    void dadoElDocumento_cuandoSeLeeCadaPagina_entoncesTieneElPieConLaVersionYElHash()
            throws Exception {
        try (PDDocument documento =
                Loader.loadPDF(generador.generar(ContenidosDePrueba.completo()))) {
            for (int pagina = 1; pagina <= documento.getNumberOfPages(); pagina++) {
                assertThat(texto(documento, pagina))
                        .as("pie de la página %d", pagina)
                        .contains("2026-I · versión 1 · hash " + ContenidosDePrueba.HASH);
            }
        }
    }

    @Test
    void dadoElContenido_cuandoSeLee_entoncesTieneElResumenLosTotalesYCadaDocente()
            throws Exception {
        ContenidoDelDocumento contenido = ContenidosDePrueba.pequeno();
        try (PDDocument documento = Loader.loadPDF(generador.generar(contenido))) {
            String todo = new PDFTextStripper().getText(documento);

            assertThat(todo)
                    .contains("Distributivo docente del periodo 2026-I")
                    .contains("DESARROLLO DE SOFTWARE")
                    .contains("CONTABILIDAD")
                    .contains("Totales institucionales")
                    .contains("Programación orientada a objetos")
                    .contains("Contabilidad general")
                    .contains("Tutorías")
                    .contains("ARIAS ANA")
                    .contains("0909090909");
            assertThat(todo.indexOf("Totales institucionales"))
                    .isLessThan(todo.indexOf("ARIAS ANA"));
        }
    }

    @Test
    void dadoUnTextoConSimbolos_cuandoSeGenera_entoncesSaleEscapado() throws Exception {
        try (PDDocument documento =
                Loader.loadPDF(generador.generar(ContenidosDePrueba.pequeno()))) {
            assertThat(new PDFTextStripper().getText(documento))
                    .contains("ZAMBRANO <LUIS> & \"HIJOS\"");
        }
    }

    // El bloque de firmas (F13-H02, FR-006, FR-007; research §2).

    @Test
    void dadoElDocumento_cuandoSeLeeLaUltimaPagina_entoncesTieneLosTresRotulosConCargoYNombre()
            throws Exception {
        try (PDDocument documento =
                Loader.loadPDF(generador.generar(ContenidosDePrueba.completo()))) {
            String ultima = texto(documento, documento.getNumberOfPages());

            assertThat(ultima)
                    .contains("Elaborado por")
                    .contains("Responsable de consolidación")
                    .contains("PÉREZ ANA")
                    .contains("Revisado por")
                    .contains("Vicerrector académico")
                    .contains("LÓPEZ LUIS")
                    .contains("Aprobado por")
                    .contains("Rector")
                    .contains("MORA EVA");
            assertThat(ultima.indexOf("Elaborado por")).isLessThan(ultima.indexOf("Revisado por"));
            assertThat(ultima.indexOf("Revisado por")).isLessThan(ultima.indexOf("Aprobado por"));
            assertThat(texto(documento, documento.getNumberOfPages() - 1))
                    .doesNotContain("Elaborado por");
        }
    }

    @Test
    void dadoElDocumento_cuandoSeLeenSusCampos_entoncesTresCamposDeFirmaVaciosEnLaUltimaPagina()
            throws Exception {
        try (PDDocument documento =
                Loader.loadPDF(generador.generar(ContenidosDePrueba.completo()))) {
            PDAcroForm formulario = documento.getDocumentCatalog().getAcroForm();
            PDPage ultima = documento.getPage(documento.getNumberOfPages() - 1);

            assertThat(formulario).isNotNull();
            assertThat(formulario.getFields())
                    .extracting(PDField::getPartialName)
                    .containsExactly(
                            "firma-1-RESPONSABLE_CONSOLIDACION",
                            "firma-2-VICERRECTOR",
                            "firma-3-RECTOR");
            for (PDField campo : formulario.getFields()) {
                assertThat(campo).isInstanceOf(PDSignatureField.class);
                assertThat(((PDSignatureField) campo).getSignature()).as("vacío").isNull();
                PDAnnotationWidget widget = campo.getWidgets().getFirst();
                assertThat(widget.isPrinted()).isTrue();
                assertThat(widget.getAppearance().getNormalAppearance()).isNotNull();
                assertThat(ultima.getAnnotations()).contains(widget);
                PDRectangle caja = widget.getRectangle();
                assertThat(caja.getWidth()).isPositive();
                assertThat(caja.getHeight()).isPositive();
                assertThat(
                                ultima.getMediaBox()
                                        .contains(caja.getLowerLeftX(), caja.getLowerLeftY()))
                        .isTrue();
                assertThat(
                                ultima.getMediaBox()
                                        .contains(caja.getUpperRightX(), caja.getUpperRightY()))
                        .isTrue();
            }
        }
    }

    @Test
    void dadosLosCamposDeFirma_cuandoSeValida_entoncesSigueSiendoPdfaDosBConforme()
            throws Exception {
        byte[] pdf = generador.generar(ContenidosDePrueba.completo());

        assertThat(VerificadorPdfa.reglasIncumplidas(pdf)).isEmpty();
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(documento.getDocumentCatalog().getAcroForm().getFields()).hasSize(3);
            // Una copia para revisarlo a mano: el PDF y la última página con los campos marcados.
            Files.createDirectories(SALIDA);
            Files.write(SALIDA.resolve("documento-de-prueba.pdf"), pdf);
            BufferedImage imagen =
                    new PDFRenderer(documento)
                            .renderImageWithDPI(documento.getNumberOfPages() - 1, 72);
            Graphics2D lapiz = imagen.createGraphics();
            lapiz.setColor(Color.RED);
            float alto =
                    documento.getPage(documento.getNumberOfPages() - 1).getMediaBox().getHeight();
            for (PDField campo : documento.getDocumentCatalog().getAcroForm().getFields()) {
                PDRectangle caja = campo.getWidgets().getFirst().getRectangle();
                lapiz.drawRect(
                        Math.round(caja.getLowerLeftX()),
                        Math.round(alto - caja.getUpperRightY()),
                        Math.round(caja.getWidth()),
                        Math.round(caja.getHeight()));
            }
            lapiz.dispose();
            ImageIO.write(imagen, "png", SALIDA.resolve("ultima-pagina.png").toFile());
            ImageIO.write(
                    new PDFRenderer(documento).renderImageWithDPI(0, 72),
                    "png",
                    SALIDA.resolve("primera-pagina.png").toFile());
        }
    }

    private static String texto(PDDocument documento, int pagina) throws Exception {
        PDFTextStripper lector = new PDFTextStripper();
        lector.setStartPage(pagina);
        lector.setEndPage(pagina);
        return lector.getText(documento);
    }
}
