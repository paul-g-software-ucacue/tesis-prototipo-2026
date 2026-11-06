package ec.mdd.generacion.puertos.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.generacion.puertos.salida.CambioDeDistributivo.Accion;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo.Clase;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Cada acción declara su clase, y el cambio la cumple (research §2, F17-H01). */
class CambioDeDistributivoTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-06T15:00:00Z");
    private static final String QUIEN = "coordinacion.demo";
    private static final Map<String, Object> DETALLE = Map.of("periodo", "2026-I");
    private static final Map<String, Object> ANTES = Map.of("horas", "4");
    private static final Map<String, Object> DESPUES = Map.of("horas", "6");

    private static CambioDeDistributivo cambio(
            Accion accion, Map<String, Object> anterior, Map<String, Object> nuevo) {
        return new CambioDeDistributivo(MOMENTO, QUIEN, accion, DETALLE, anterior, nuevo);
    }

    @Test
    void cadaAccion_cuandoSeConsultaSuClase_entoncesEsLaDelInventario() {
        assertThat(Accion.DISTRIBUTIVO_CREADO.clase()).isEqualTo(Clase.CREA);
        assertThat(Accion.PROPUESTA_GUARDADA.clase()).isEqualTo(Clase.CREA_O_CAMBIA);
        assertThat(Accion.ACTIVIDAD_GUARDADA.clase()).isEqualTo(Clase.CREA_O_CAMBIA);
        assertThat(Accion.INCONSISTENCIA_JUSTIFICADA.clase()).isEqualTo(Clase.CREA_O_CAMBIA);
        assertThat(Accion.DISTRIBUTIVO_CONSOLIDADO.clase()).isEqualTo(Clase.CREA_O_CAMBIA);
        assertThat(Accion.INFORME_ADJUNTADO.clase()).isEqualTo(Clase.CREA_O_CAMBIA);
        assertThat(Accion.PROPUESTA_QUITADA.clase()).isEqualTo(Clase.QUITA);
        assertThat(Accion.ACTIVIDAD_QUITADA.clase()).isEqualTo(Clase.QUITA);
    }

    @Test
    void unaActividadDe4A6Horas_cuandoSeRegistra_entoncesGuardaElAnteriorYElNuevo() {
        CambioDeDistributivo cambio = cambio(Accion.ACTIVIDAD_GUARDADA, ANTES, DESPUES);

        assertThat(cambio.anterior()).containsExactlyEntriesOf(ANTES);
        assertThat(cambio.nuevo()).containsExactlyEntriesOf(DESPUES);
    }

    @Test
    void unaAccionQueCrea_cuandoTraeAnterior_entoncesSeRechaza() {
        assertThatThrownBy(() -> cambio(Accion.DISTRIBUTIVO_CREADO, ANTES, DESPUES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DISTRIBUTIVO_CREADO");
    }

    @Test
    void unaAccionQueCrea_cuandoNoTraeNuevo_entoncesSeRechaza() {
        assertThatThrownBy(() -> cambio(Accion.DISTRIBUTIVO_CREADO, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unaAccionQueQuita_cuandoTraeNuevo_entoncesSeRechaza() {
        assertThatThrownBy(() -> cambio(Accion.ACTIVIDAD_QUITADA, ANTES, DESPUES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACTIVIDAD_QUITADA");
    }

    @Test
    void unaAccionQueQuita_cuandoNoTraeAnterior_entoncesSeRechaza() {
        assertThatThrownBy(() -> cambio(Accion.PROPUESTA_QUITADA, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(
            value = Accion.class,
            names = {
                "PROPUESTA_GUARDADA",
                "ACTIVIDAD_GUARDADA",
                "INCONSISTENCIA_JUSTIFICADA",
                "DISTRIBUTIVO_CONSOLIDADO",
                "INFORME_ADJUNTADO"
            })
    void unaAccionQueCreaOCambia_cuandoTraeNuevo_entoncesValeConOSinAnterior(Accion accion) {
        assertThat(cambio(accion, Map.of(), DESPUES).anterior()).isEmpty();
        assertThat(cambio(accion, ANTES, DESPUES).anterior()).isEqualTo(ANTES);
    }

    @ParameterizedTest
    @EnumSource(
            value = Accion.class,
            names = {
                "PROPUESTA_GUARDADA",
                "ACTIVIDAD_GUARDADA",
                "INCONSISTENCIA_JUSTIFICADA",
                "DISTRIBUTIVO_CONSOLIDADO",
                "INFORME_ADJUNTADO"
            })
    void unaAccionQueCreaOCambia_cuandoNoTraeNuevo_entoncesSeRechaza(Accion accion) {
        assertThatThrownBy(() -> cambio(accion, ANTES, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void losValores_cuandoSeModificaElMapaOriginal_entoncesNoCambian() {
        Map<String, Object> nuevo = new java.util.HashMap<>(DESPUES);
        CambioDeDistributivo cambio = cambio(Accion.ACTIVIDAD_GUARDADA, ANTES, nuevo);
        nuevo.put("horas", "8");

        assertThat(cambio.nuevo()).containsEntry("horas", "6");
        assertThatThrownBy(() -> cambio.nuevo().put("horas", "8"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
