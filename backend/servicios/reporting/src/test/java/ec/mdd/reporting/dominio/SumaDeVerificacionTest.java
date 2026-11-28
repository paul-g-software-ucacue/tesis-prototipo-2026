package ec.mdd.reporting.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** El SHA-256 del CSV entregado, en hexadecimal en minúsculas (F19-H02, FR-009, D-120). */
class SumaDeVerificacionTest {

    @Test
    void dadosLosBytesDeAbc_cuandoSeCalcula_entoncesEsElVectorConocidoDeSha256() {
        assertThat(SumaDeVerificacion.de("abc".getBytes(StandardCharsets.US_ASCII)).valor())
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void dadoUnArchivoVacio_cuandoSeCalcula_entoncesEsElSha256DelVacio() {
        assertThat(SumaDeVerificacion.de(new byte[0]).valor())
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void dadoUnValorConMayusculasODeOtroLargo_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> new SumaDeVerificacion("A".repeat(64)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SumaDeVerificacion("a".repeat(63)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
