package ec.mdd.firma.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RechazoDelFirmanteTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-15T10:00:00Z");

    private static RechazoDelFirmante rechazo(String observacion) {
        return new RechazoDelFirmante(2, "vicerrectorado.demo", MOMENTO, observacion);
    }

    @Test
    void dadaUnaObservacionConEspaciosEnLosExtremos_cuandoSeCrea_entoncesSeGuardaRecortada() {
        assertThat(rechazo("   Falta la carga de Software.  ").observacion())
                .isEqualTo("Falta la carga de Software.");
    }

    @Test
    void dadasObservacionesDeDiezYQuinientosCaracteres_cuandoSeCrean_entoncesSeAceptan() {
        assertThat(rechazo("a".repeat(10)).observacion()).hasSize(10);
        assertThat(rechazo("a".repeat(500)).observacion()).hasSize(500);
    }

    @Test
    void dadaUnaObservacionVaciaCortaOLarga_cuandoSeCrea_entoncesEsInvalida() {
        assertThatThrownBy(() -> rechazo("")).isInstanceOf(ObservacionInvalida.class);
        assertThatThrownBy(() -> rechazo("          ")).isInstanceOf(ObservacionInvalida.class);
        assertThatThrownBy(() -> rechazo("  " + "a".repeat(9) + "  "))
                .isInstanceOf(ObservacionInvalida.class);
        assertThatThrownBy(() -> rechazo("a".repeat(501))).isInstanceOf(ObservacionInvalida.class);
        assertThatThrownBy(() -> rechazo(null)).isInstanceOf(ObservacionInvalida.class);
    }

    @Test
    void dadoUnPasoOUnAutorInvalidos_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                new RechazoDelFirmante(
                                        0, "vicerrectorado.demo", MOMENTO, "a".repeat(10)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RechazoDelFirmante(1, null, MOMENTO, "a".repeat(10)))
                .isInstanceOf(NullPointerException.class);
    }
}
