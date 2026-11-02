package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** El texto de una justificación tiene de 10 a 500 caracteres, sin contar los extremos. */
class JustificacionDeInconsistenciaTest {

    @Test
    void conEspaciosEnLosExtremos_cuandoSeCrea_entoncesLosQuita() {
        assertThat(nueva("  Es por su carga de tronco común  ").texto())
                .isEqualTo("Es por su carga de tronco común");
    }

    @Test
    void dadosLosLimites_cuandoSeCrea_entoncesDiezYQuinientosPasan() {
        assertThat(nueva("x".repeat(10)).texto()).hasSize(10);
        assertThat(nueva("x".repeat(500)).texto()).hasSize(500);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9, 501})
    void conOtroLargo_cuandoSeCrea_entoncesLoRechaza(int largo) {
        assertThatThrownBy(() -> nueva("x".repeat(largo)))
                .isInstanceOf(JustificacionObligatoria.class);
    }

    @Test
    void conNueveCaracteresYEspacios_cuandoSeCrea_entoncesLoRechaza() {
        assertThatThrownBy(() -> nueva("  xxxxxxxxx   "))
                .isInstanceOf(JustificacionObligatoria.class);
    }

    @Test
    void sinTexto_cuandoSeCrea_entoncesLoRechaza() {
        assertThatThrownBy(() -> nueva(null)).isInstanceOf(JustificacionObligatoria.class);
    }

    private static JustificacionDeInconsistencia nueva(String texto) {
        return JustificacionDeInconsistencia.nueva(
                TipoDeInconsistencia.PROPORCION,
                "0109990010",
                texto,
                "consolidacion.demo",
                Instant.parse("2026-11-02T15:00:00Z"));
    }
}
