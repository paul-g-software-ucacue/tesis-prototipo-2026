package ec.mdd.firma.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArchivoSubidoTest {

    @Test
    void dadoElLimite_cuandoSeConsulta_entoncesEsDiezMegabytes() {
        assertThat(ArchivoDemasiadoGrande.LIMITE_EN_BYTES).isEqualTo(10 * 1024 * 1024);
    }
}
