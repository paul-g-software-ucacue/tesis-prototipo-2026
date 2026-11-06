package ec.mdd.validacionocs.puertos.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo.Accion;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** La carga de la configuración crea la primera vez y cambia después (research §2, F17-H01). */
class CambioDeFlujoTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-06T15:00:00Z");
    private static final Map<String, Object> DETALLE = Map.of("periodo", "2026-I");

    private static CambioDeFlujo cambio(Map<String, Object> anterior, Map<String, Object> nuevo) {
        return new CambioDeFlujo(
                MOMENTO,
                "vicerrectorado.demo",
                Accion.CONFIGURACION_FLUJO_CARGADA,
                DETALLE,
                anterior,
                nuevo);
    }

    @Test
    void laPrimeraCarga_cuandoSeRegistra_entoncesSoloLlevaNuevo() {
        CambioDeFlujo cambio = cambio(Map.of(), Map.of("version", 1));

        assertThat(cambio.anterior()).isEmpty();
        assertThat(cambio.nuevo()).isEqualTo(Map.of("version", 1));
    }

    @Test
    void otraCarga_cuandoSeRegistra_entoncesLlevaLaVersionAnteriorYLaNueva() {
        CambioDeFlujo cambio = cambio(Map.of("version", 1), Map.of("version", 2));

        assertThat(cambio.anterior()).isEqualTo(Map.of("version", 1));
        assertThat(cambio.nuevo()).isEqualTo(Map.of("version", 2));
    }

    @Test
    void unaCargaSinNuevo_cuandoSeRegistra_entoncesSeRechaza() {
        assertThatThrownBy(() -> cambio(Map.of("version", 1), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CONFIGURACION_FLUJO_CARGADA");
    }

    /** FR-011 y T025 de F17-H01: ninguna acción registra una clave que parece un secreto. */
    @ParameterizedTest
    @EnumSource(Accion.class)
    void cadaAccion_cuandoUnaClaveParecesUnSecreto_entoncesSeRechaza(Accion accion) {
        Map<String, Object> anidado =
                Map.of("cambios", List.of(Map.of("campo", "x", "token", "y")));

        assertThatThrownBy(
                        () ->
                                new CambioDeFlujo(
                                        MOMENTO,
                                        "vicerrectorado.demo",
                                        accion,
                                        anidado,
                                        Map.of(),
                                        Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secreto")
                .message()
                .doesNotContain("y\"");
        assertThatThrownBy(
                        () ->
                                new CambioDeFlujo(
                                        MOMENTO,
                                        "vicerrectorado.demo",
                                        accion,
                                        Map.of("x", "y"),
                                        Map.of("password", "y"),
                                        Map.of("claveDeAcceso", "y")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secreto");
    }
}
