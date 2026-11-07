package ec.mdd.normativa.puertos.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.normativa.puertos.salida.CambioDeNormativa.Accion;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa.Clase;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Cada acción declara su clase, y el cambio la cumple (research §2, F17-H01). */
class CambioDeNormativaTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-06T15:00:00Z");
    private static final String QUIEN = "vicerrectorado.demo";
    private static final Map<String, Object> DETALLE = Map.of("numero", 2);
    private static final Map<String, Object> ANTES = Map.of("codigo", "POL-2024");
    private static final Map<String, Object> DESPUES = Map.of("codigo", "POL-2025");

    private static CambioDeNormativa cambio(
            Accion accion, Map<String, Object> anterior, Map<String, Object> nuevo) {
        return new CambioDeNormativa(MOMENTO, QUIEN, accion, DETALLE, anterior, nuevo);
    }

    @Test
    void cadaAccion_cuandoSeConsultaSuClase_entoncesEsLaDelInventario() {
        assertThat(Accion.VERSION_CREADA.clase()).isEqualTo(Clase.CREA);
        assertThat(Accion.VERSION_EDITADA.clase()).isEqualTo(Clase.CAMBIA);
        assertThat(Accion.VERSION_ASOCIADA.clase()).isEqualTo(Clase.CREA_O_CAMBIA);
    }

    @Test
    void unaEdicion_cuandoUnParametroNoTeniaValor_entoncesQuedaComoNulo() {
        Map<String, Object> antes = new HashMap<>();
        antes.put("reglas.RN-19.minimo", null);

        CambioDeNormativa cambio =
                cambio(Accion.VERSION_EDITADA, antes, Map.of("reglas.RN-19.minimo", "0.5"));

        assertThat(cambio.anterior()).containsEntry("reglas.RN-19.minimo", null);
        assertThat(cambio.nuevo()).containsEntry("reglas.RN-19.minimo", "0.5");
    }

    @Test
    void unaVersionCreada_cuandoTraeAnteriorOLeFaltaElNuevo_entoncesSeRechaza() {
        assertThatThrownBy(() -> cambio(Accion.VERSION_CREADA, ANTES, DESPUES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VERSION_CREADA");
        assertThatThrownBy(() -> cambio(Accion.VERSION_CREADA, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unaEdicion_cuandoLeFaltaElAnteriorOElNuevo_entoncesSeRechaza() {
        assertThatThrownBy(() -> cambio(Accion.VERSION_EDITADA, Map.of(), DESPUES))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cambio(Accion.VERSION_EDITADA, ANTES, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unaAsociacion_cuandoTraeNuevo_entoncesValeConOSinAnteriorYSinNuevoNo() {
        assertThat(cambio(Accion.VERSION_ASOCIADA, Map.of(), Map.of("version", 2)).anterior())
                .isEmpty();
        assertThat(
                        cambio(Accion.VERSION_ASOCIADA, Map.of("version", 1), Map.of("version", 2))
                                .anterior())
                .containsEntry("version", 1);
        assertThatThrownBy(() -> cambio(Accion.VERSION_ASOCIADA, Map.of("version", 1), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** FR-011 y T025 de F17-H01: ninguna acción registra una clave que parece un secreto. */
    @ParameterizedTest
    @EnumSource(Accion.class)
    void cadaAccion_cuandoUnaClaveParecesUnSecreto_entoncesSeRechaza(Accion accion) {
        Map<String, Object> anidado =
                Map.of("cambios", List.of(Map.of("campo", "x", "token", "y")));

        assertThatThrownBy(
                        () ->
                                new CambioDeNormativa(
                                        MOMENTO, QUIEN, accion, anidado, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secreto")
                .message()
                .doesNotContain("y\"");
        assertThatThrownBy(
                        () ->
                                new CambioDeNormativa(
                                        MOMENTO,
                                        QUIEN,
                                        accion,
                                        Map.of("x", "y"),
                                        Map.of("password", "y"),
                                        Map.of("claveDeAcceso", "y")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secreto");
    }
}
