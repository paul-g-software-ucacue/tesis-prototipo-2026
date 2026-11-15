package ec.mdd.firma.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class FirmanteEsperadoTest {

    @Test
    void dadoUnFirmanteSinPersonaOConLasDos_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> new FirmanteEsperado(1, "RECTOR", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FirmanteEsperado(1, "RECTOR", "rectorado.demo", "0109990010"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FirmanteEsperado.porIdentificacion(1, "RECTOR", "abc-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoElResponsableDesignado_cuandoPideSuUsuario_entoncesLoReconoceSinMirarElRol() {
        FirmanteEsperado responsable =
                FirmanteEsperado.porUsuario(1, "RESPONSABLE_CONSOLIDACION", "docente.demo");

        assertThat(responsable.reconoce("docente.demo", Optional.empty())).isTrue();
        assertThat(responsable.reconoce("DOCENTE.DEMO", Optional.empty())).isTrue();
        assertThat(responsable.reconoce("consolidacion.demo", Optional.empty())).isFalse();
        assertThat(responsable.porIdentificacion()).isFalse();
    }

    @Test
    void dadoUnCargoDeAutoridad_cuandoPideAlguien_entoncesLoReconocePorSuIdentificacion() {
        FirmanteEsperado vicerrector =
                FirmanteEsperado.porIdentificacion(2, "VICERRECTOR", "0109990028");

        assertThat(vicerrector.porIdentificacion()).isTrue();
        assertThat(vicerrector.reconoce("vicerrectorado.demo", Optional.of("0109990028"))).isTrue();
        assertThat(vicerrector.reconoce("vicerrectorado.demo", Optional.of("0109990010")))
                .isFalse();
        assertThat(vicerrector.reconoce("vicerrectorado.demo", Optional.empty())).isFalse();
    }

    @Test
    void dadoUnFirmanteConSuNombreImpreso_cuandoSeCrea_entoncesLoGuarda() {
        assertThat(
                        FirmanteEsperado.porIdentificacion(
                                        2,
                                        "VICERRECTOR",
                                        "0109990028",
                                        "CABRERA VAZQUEZ ANA GABRIELA")
                                .nombre())
                .contains("CABRERA VAZQUEZ ANA GABRIELA");
        assertThat(
                        FirmanteEsperado.porUsuario(1, "RESPONSABLE_CONSOLIDACION", "docente.demo")
                                .nombre())
                .isEmpty();
    }

    @Test
    void dadoUnNombreDeMasDe200Caracteres_cuandoSeCrea_entoncesSeRechaza() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () ->
                                FirmanteEsperado.porUsuario(
                                        1,
                                        "RESPONSABLE_CONSOLIDACION",
                                        "docente.demo",
                                        "x".repeat(201)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
