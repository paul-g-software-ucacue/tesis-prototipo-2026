package ec.mdd.firma.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ComprobacionDeDocumentoTest {

    private static final byte[] VIGENTE = "%PDF-1.7 vigente %%EOF".getBytes(StandardCharsets.US_ASCII);

    private static byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void dadoElVigenteConAlgoAgregado_cuandoSeComprueba_entoncesEsElMismoDocumento() {
        ResultadoDeComprobacion resultado =
                ComprobacionDeDocumento.mismoDocumento(
                        VIGENTE, bytes("%PDF-1.7 vigente %%EOF firma nueva %%EOF"));

        assertThat(resultado.comprobacion()).isEqualTo(Comprobacion.MISMO_DOCUMENTO);
        assertThat(resultado.resultado()).isEqualTo(Resultado.APROBADA);
    }

    @Test
    void dadoElVigenteSinCambios_cuandoSeComprueba_entoncesPasaYLaFirmaNuevaLaRevisaLaSiguiente() {
        assertThat(ComprobacionDeDocumento.mismoDocumento(VIGENTE, VIGENTE.clone()).resultado())
                .isEqualTo(Resultado.APROBADA);
    }

    @Test
    void dadoUnArchivoMasCortoQueElVigente_cuandoSeComprueba_entoncesSeRechaza() {
        ResultadoDeComprobacion resultado =
                ComprobacionDeDocumento.mismoDocumento(VIGENTE, bytes("%PDF-1.7 vigente"));

        assertThat(resultado.resultado()).isEqualTo(Resultado.RECHAZADA);
        assertThat(resultado.motivo()).isPresent();
    }

    @Test
    void dadoUnArchivoConOtroPrefijo_cuandoSeComprueba_entoncesSeRechaza() {
        assertThat(
                        ComprobacionDeDocumento.mismoDocumento(
                                        VIGENTE, bytes("%PDF-1.7 VIGENTE %%EOF firma nueva %%EOF"))
                                .resultado())
                .isEqualTo(Resultado.RECHAZADA);
    }

    @Test
    void dadoUnArchivoQueSoloCambiaUnByteAlFinalDelVigente_cuandoSeComprueba_entoncesSeRechaza() {
        assertThat(
                        ComprobacionDeDocumento.mismoDocumento(
                                        VIGENTE, bytes("%PDF-1.7 vigente %%EOG firma nueva"))
                                .resultado())
                .isEqualTo(Resultado.RECHAZADA);
    }
}
