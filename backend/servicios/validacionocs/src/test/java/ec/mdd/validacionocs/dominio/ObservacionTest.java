package ec.mdd.validacionocs.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** La observación de una devolución (RF-36, F12-H05): de 10 a 500 caracteres sin los extremos. */
class ObservacionTest {

    @Test
    void dadaUnaObservacionDe10a500_cuandoSeArma_entoncesSeGuardaSinLosEspaciosDeLosExtremos() {
        assertThat(DevolucionDeParcial.de("DS", "  " + "a".repeat(10) + "\n ").observacion())
                .isEqualTo(new Observacion("a".repeat(10)));
        assertThat(new Observacion("a".repeat(500)).texto()).hasSize(500);
        assertThat(new Observacion("Falta\ndocente").texto()).isEqualTo("Falta\ndocente");
    }

    @Test
    void dadaUnaObservacionCorta_cuandoSeArma_entoncesSeRechazaNombrandoLaCarrera() {
        assertThatThrownBy(() -> DevolucionDeParcial.de("DS", "a".repeat(9)))
                .isInstanceOf(ObservacionInvalida.class)
                .extracting(error -> ((ObservacionInvalida) error).carrera())
                .isEqualTo("DS");
        assertThatThrownBy(() -> DevolucionDeParcial.de("DS", "   " + "a".repeat(9) + "   "))
                .isInstanceOf(ObservacionInvalida.class);
        assertThatThrownBy(() -> DevolucionDeParcial.de("DS", " ".repeat(20)))
                .isInstanceOf(ObservacionInvalida.class);
        assertThatThrownBy(() -> DevolucionDeParcial.de("DS", null))
                .isInstanceOf(ObservacionInvalida.class);
    }

    @Test
    void dadaUnaObservacionLarga_cuandoSeArma_entoncesSeRechaza() {
        assertThatThrownBy(() -> DevolucionDeParcial.de("DS", "a".repeat(501)))
                .isInstanceOf(ObservacionInvalida.class);
    }
}
