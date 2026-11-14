package ec.mdd.firma.adaptadores.salida;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;

/**
 * Valida un PDF con veraPDF contra PDF/A-2b, como el spike B00-S01 (D-46). Solo en pruebas: veraPDF
 * necesita JAXB 2.3 y no entra al proceso (D-101).
 */
final class VerificadorPdfa {

    private VerificadorPdfa() {}

    /**
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
}
