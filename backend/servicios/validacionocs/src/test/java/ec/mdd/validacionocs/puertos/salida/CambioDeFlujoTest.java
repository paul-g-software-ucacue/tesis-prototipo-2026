package ec.mdd.validacionocs.puertos.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo.Accion;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

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
}
