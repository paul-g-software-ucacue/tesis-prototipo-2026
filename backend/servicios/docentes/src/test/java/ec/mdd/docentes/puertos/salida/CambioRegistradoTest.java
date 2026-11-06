package ec.mdd.docentes.puertos.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.docentes.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.docentes.puertos.salida.CambioRegistrado.Clase;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Cada acción declara su clase, y el cambio la cumple (research §2, F17-H01). */
class CambioRegistradoTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-06T15:00:00Z");
    private static final String QUIEN = "talento.demo";
    private static final Map<String, Object> DETALLE = Map.of("identificacion", "0100000001");
    private static final Map<String, Object> ANTES = Map.of("dedicacion", "MEDIO_TIEMPO");
    private static final Map<String, Object> DESPUES = Map.of("dedicacion", "TIEMPO_COMPLETO");

    private static CambioRegistrado cambio(
            Accion accion, Map<String, Object> anterior, Map<String, Object> nuevo) {
        return new CambioRegistrado(MOMENTO, QUIEN, accion, DETALLE, anterior, nuevo);
    }

    @Test
    void cadaAccion_cuandoSeConsultaSuClase_entoncesRegistrarCreaYLoDemasCambia() {
        for (Accion accion : Accion.values()) {
            assertThat(accion.clase())
                    .as(accion.name())
                    .isEqualTo(accion.name().endsWith("_REGISTRADO") ? Clase.CREA : Clase.CAMBIA);
        }
    }

    @Test
    void unaCorreccion_cuandoSeRegistra_entoncesGuardaElAnteriorYElNuevo() {
        CambioRegistrado cambio = cambio(Accion.DOCENTE_CORREGIDO, ANTES, DESPUES);

        assertThat(cambio.anterior()).containsExactlyEntriesOf(ANTES);
        assertThat(cambio.nuevo()).containsExactlyEntriesOf(DESPUES);
    }

    @Test
    void unValorQueAntesNoExistia_cuandoSeRegistra_entoncesQuedaComoNulo() {
        Map<String, Object> sinFin = new HashMap<>();
        sinFin.put("finContrato", null);

        assertThat(
                        cambio(
                                        Accion.DOCENTE_CORREGIDO,
                                        sinFin,
                                        Map.of("finContrato", "2027-01-01"))
                                .anterior())
                .containsEntry("finContrato", null);
    }

    @ParameterizedTest
    @EnumSource(
            value = Accion.class,
            names = {".*_REGISTRADO"},
            mode = EnumSource.Mode.MATCH_ANY)
    void unaAccionQueCrea_cuandoTraeAnteriorOLeFaltaElNuevo_entoncesSeRechaza(Accion accion) {
        assertThatThrownBy(() -> cambio(accion, ANTES, DESPUES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(accion.name());
        assertThatThrownBy(() -> cambio(accion, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(
            value = Accion.class,
            names = {".*_REGISTRADO"},
            mode = EnumSource.Mode.MATCH_NONE)
    void unaAccionQueCambia_cuandoLeFaltaElAnteriorOElNuevo_entoncesSeRechaza(Accion accion) {
        assertThatThrownBy(() -> cambio(accion, Map.of(), DESPUES))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cambio(accion, ANTES, Map.of()))
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
                                new CambioRegistrado(
                                        MOMENTO, QUIEN, accion, anidado, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secreto")
                .message()
                .doesNotContain("y\"");
        assertThatThrownBy(
                        () ->
                                new CambioRegistrado(
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
