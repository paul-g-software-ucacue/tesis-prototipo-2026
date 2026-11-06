package ec.mdd.academico.puertos.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Clase;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Cada acción declara su clase, y el cambio la cumple (research §2, F17-H01). */
class CambioRegistradoTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-06T15:00:00Z");
    private static final String QUIEN = "vicerrectorado.demo";
    private static final Map<String, Object> DETALLE = Map.of("codigo", "2026-I");
    private static final Map<String, Object> ANTES = Map.of("estado", "PLANIFICACION");
    private static final Map<String, Object> DESPUES = Map.of("estado", "EN_CURSO");

    private static CambioRegistrado cambio(
            Accion accion, Map<String, Object> anterior, Map<String, Object> nuevo) {
        return new CambioRegistrado(MOMENTO, QUIEN, accion, DETALLE, anterior, nuevo);
    }

    @Test
    void lasAccionesQueRegistran_cuandoSeConsultaSuClase_entoncesCrean() {
        assertThat(Arrays.stream(Accion.values()).filter(una -> una.clase() == Clase.CREA))
                .containsExactlyInAnyOrder(
                        Accion.PERIODO_REGISTRADO,
                        Accion.UNIDAD_REGISTRADA,
                        Accion.CARRERA_REGISTRADA,
                        Accion.MALLA_REGISTRADA,
                        Accion.ASIGNATURA_REGISTRADA,
                        Accion.ASIGNATURA_PARALELO_REGISTRADA,
                        Accion.MATRICULA_REGISTRADA);
    }

    @Test
    void lasDemasAcciones_cuandoSeConsultaSuClase_entoncesCambian() {
        assertThat(Arrays.stream(Accion.values()).filter(una -> una.clase() == Clase.CAMBIA))
                .hasSize(Accion.values().length - 7)
                .contains(
                        Accion.PERIODO_ABIERTO,
                        Accion.PERIODO_CORREGIDO,
                        Accion.UNIDAD_INACTIVADA,
                        Accion.ASIGNATURA_PARALELO_RETIRADA,
                        Accion.MATRICULA_CORREGIDA);
    }

    @Test
    void unCambioDeEstado_cuandoSeRegistra_entoncesGuardaElAnteriorYElNuevo() {
        CambioRegistrado cambio = cambio(Accion.PERIODO_ESTADO_CAMBIADO, ANTES, DESPUES);

        assertThat(cambio.anterior()).containsExactlyEntriesOf(ANTES);
        assertThat(cambio.nuevo()).containsExactlyEntriesOf(DESPUES);
    }

    @Test
    void unValorQueAntesNoExistia_cuandoSeRegistra_entoncesQuedaComoNulo() {
        Map<String, Object> sinPadre = new HashMap<>();
        sinPadre.put("padre", null);

        CambioRegistrado cambio =
                cambio(Accion.UNIDAD_CORREGIDA, sinPadre, Map.of("padre", "FACULTAD"));

        assertThat(cambio.anterior()).containsEntry("padre", null);
    }

    @ParameterizedTest
    @EnumSource(
            value = Accion.class,
            names = {".*_REGISTRADA", "PERIODO_REGISTRADO"},
            mode = EnumSource.Mode.MATCH_ANY)
    void unaAccionQueCrea_cuandoTraeAnterior_entoncesSeRechaza(Accion accion) {
        assertThatThrownBy(() -> cambio(accion, ANTES, DESPUES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(accion.name());
    }

    @ParameterizedTest
    @EnumSource(
            value = Accion.class,
            names = {".*_REGISTRADA", "PERIODO_REGISTRADO"},
            mode = EnumSource.Mode.MATCH_ANY)
    void unaAccionQueCrea_cuandoNoTraeNuevo_entoncesSeRechaza(Accion accion) {
        assertThatThrownBy(() -> cambio(accion, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(
            value = Accion.class,
            names = {".*_REGISTRADA", "PERIODO_REGISTRADO"},
            mode = EnumSource.Mode.MATCH_NONE)
    void unaAccionQueCambia_cuandoLeFaltaElAnteriorOElNuevo_entoncesSeRechaza(Accion accion) {
        assertThatThrownBy(() -> cambio(accion, Map.of(), DESPUES))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cambio(accion, ANTES, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void losValores_cuandoSeIntentaModificarlos_entoncesNoSePuede() {
        CambioRegistrado cambio = cambio(Accion.PERIODO_ESTADO_CAMBIADO, ANTES, DESPUES);

        assertThatThrownBy(() -> cambio.nuevo().put("estado", "CERRADO"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
