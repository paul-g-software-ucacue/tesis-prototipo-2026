package ec.mdd.firma.spike;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.firma.spike.FirmadorDePrueba.Identidad;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Casos de la validación que no dependen de FirmaEC (FR-007, SC-002, contracts/validacion.md): un
 * caso positivo con dos firmas, un byte alterado, otra raíz de confianza y un certificado vencido.
 * Las firmas las genera la propia prueba con PDFBox y Bouncy Castle, así que corren siempre en la
 * CI.
 */
class CasosNegativosTest {

    private static final ValidadorPadesDePrueba VALIDADOR = new ValidadorPadesDePrueba();

    private static Identidad ca;
    private static Identidad f1;
    private static Identidad f2;
    private static Instant ahora;

    @BeforeAll
    static void emitirCertificados() throws Exception {
        ahora = Instant.now();
        ca = FirmadorDePrueba.ca("CA de Prueba en Memoria");
        f1 =
                FirmadorDePrueba.firmante(
                        ca,
                        "Firmante Uno Prueba (F1)",
                        ahora.minus(Duration.ofDays(1)),
                        ahora.plus(Duration.ofDays(365)));
        f2 =
                FirmadorDePrueba.firmante(
                        ca,
                        "Firmante Dos Prueba (F2)",
                        ahora.minus(Duration.ofDays(1)),
                        ahora.plus(Duration.ofDays(365)));
    }

    @Test
    void dosFirmasValidasEIncrementales() throws Exception {
        byte[] original = FirmadorDePrueba.pdfSinFirmas();
        byte[] unaFirma = FirmadorDePrueba.firmar(original, f1, ahora);
        byte[] dosFirmas = FirmadorDePrueba.firmar(unaFirma, f2, ahora.plusSeconds(60));

        List<ResultadoFirma> resultados = VALIDADOR.validar(dosFirmas, ca.certificado());

        assertThat(Arrays.equals(dosFirmas, 0, unaFirma.length, unaFirma, 0, unaFirma.length))
                .as("prefijo de bytes")
                .isTrue();
        assertThat(resultados)
                .hasSize(2)
                .allSatisfy(r -> assertThat(r.esValida()).as(r.toString()).isTrue());
        assertThat(resultados)
                .extracting(ResultadoFirma::firmante)
                .containsExactly("Firmante Uno Prueba (F1)", "Firmante Dos Prueba (F2)");
        int[] primera = ValidadorPadesDePrueba.byteRanges(dosFirmas).get(0);
        assertThat(primera[2] + primera[3]).as("revisión cerrada").isEqualTo(unaFirma.length);
    }

    @Test
    void unByteAlteradoRompeLaIntegridad() throws Exception {
        byte[] firmado = FirmadorDePrueba.firmar(FirmadorDePrueba.pdfSinFirmas(), f1, ahora);
        int[] rango = ValidadorPadesDePrueba.byteRanges(firmado).get(0);
        byte[] alterado = firmado.clone();
        // Byte 11: el comentario binario de la cabecera, dentro del primer tramo del ByteRange;
        // cambiarlo no impide leer el PDF.
        assertThat(11).isLessThan(rango[1]);
        alterado[11] ^= 0x01;

        ResultadoFirma resultado = VALIDADOR.validar(alterado, ca.certificado()).get(0);

        assertThat(resultado.integridad().valida()).as(resultado.toString()).isFalse();
    }

    @Test
    void otraRaizNoValidaLaCadena() throws Exception {
        Identidad otra = FirmadorDePrueba.ca("Otra CA de Prueba");
        byte[] firmado = FirmadorDePrueba.firmar(FirmadorDePrueba.pdfSinFirmas(), f1, ahora);

        ResultadoFirma resultado = VALIDADOR.validar(firmado, otra.certificado()).get(0);

        assertThat(resultado.integridad().valida()).isTrue();
        assertThat(resultado.cadena().valida()).as(resultado.toString()).isFalse();
    }

    @Test
    void unCertificadoVencidoNoEstaVigente() throws Exception {
        Identidad vencido =
                FirmadorDePrueba.firmante(
                        ca,
                        "Firmante Vencido Prueba",
                        ahora.minus(Duration.ofDays(30)),
                        ahora.minus(Duration.ofDays(2)));
        byte[] firmado = FirmadorDePrueba.firmar(FirmadorDePrueba.pdfSinFirmas(), vencido, ahora);

        ResultadoFirma resultado = VALIDADOR.validar(firmado, ca.certificado()).get(0);

        assertThat(resultado.integridad().valida()).isTrue();
        assertThat(resultado.vigencia().valida()).as(resultado.toString()).isFalse();
    }
}
