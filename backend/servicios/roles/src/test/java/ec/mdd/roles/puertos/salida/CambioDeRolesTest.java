package ec.mdd.roles.puertos.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.roles.puertos.salida.CambioDeRoles.Accion;
import ec.mdd.roles.puertos.salida.CambioDeRoles.Clase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Cada acción declara su clase, y el cambio la cumple (research §2, F17-H01). */
class CambioDeRolesTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-06T15:00:00Z");
    private static final String QUIEN = "admin.demo";
    private static final Map<String, Object> DETALLE = Map.of("usuario", "coordinacion.demo");
    private static final Map<String, Object> ANTES = Map.of("roles", List.of("DOCENTE"));
    private static final Map<String, Object> DESPUES =
            Map.of("roles", List.of("COORDINADOR", "DOCENTE"));

    private static CambioDeRoles cambio(
            Accion accion, Map<String, Object> anterior, Map<String, Object> nuevo) {
        return new CambioDeRoles(MOMENTO, QUIEN, accion, DETALLE, anterior, nuevo);
    }

    @Test
    void cadaAccion_cuandoSeConsultaSuClase_entoncesEsLaDelInventario() {
        assertThat(Accion.CREAR_ROL.clase()).isEqualTo(Clase.CREA);
        assertThat(Accion.INICIALIZAR.clase()).isEqualTo(Clase.CREA);
        assertThat(Accion.EDITAR_ROL.clase()).isEqualTo(Clase.CAMBIA);
        assertThat(Accion.ASIGNAR_ROL.clase()).isEqualTo(Clase.CAMBIA);
        assertThat(Accion.QUITAR_ROL.clase()).isEqualTo(Clase.CAMBIA);
        assertThat(Accion.INDICAR_CARRERA.clase()).isEqualTo(Clase.CREA_O_CAMBIA);
        assertThat(Accion.DESIGNAR_RESPONSABLE.clase()).isEqualTo(Clase.CREA_O_CAMBIA);
        assertThat(Accion.BORRAR_ROL.clase()).isEqualTo(Clase.QUITA);
        assertThat(Accion.QUITAR_CARRERA.clase()).isEqualTo(Clase.QUITA);
    }

    @Test
    void asignarUnRol_cuandoSeRegistra_entoncesGuardaLosRolesAntesYDespues() {
        CambioDeRoles cambio = cambio(Accion.ASIGNAR_ROL, ANTES, DESPUES);

        assertThat(cambio.anterior()).isEqualTo(ANTES);
        assertThat(cambio.nuevo()).isEqualTo(DESPUES);
    }

    @ParameterizedTest
    @EnumSource(
            value = Accion.class,
            names = {"CREAR_ROL", "INICIALIZAR"})
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
            names = {"EDITAR_ROL", "ASIGNAR_ROL", "QUITAR_ROL"})
    void unaAccionQueCambia_cuandoLeFaltaElAnteriorOElNuevo_entoncesSeRechaza(Accion accion) {
        assertThatThrownBy(() -> cambio(accion, Map.of(), DESPUES))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cambio(accion, ANTES, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(
            value = Accion.class,
            names = {"BORRAR_ROL", "QUITAR_CARRERA"})
    void unaAccionQueQuita_cuandoTraeNuevoOLeFaltaElAnterior_entoncesSeRechaza(Accion accion) {
        assertThatThrownBy(() -> cambio(accion, ANTES, DESPUES))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cambio(accion, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(
            value = Accion.class,
            names = {"INDICAR_CARRERA", "DESIGNAR_RESPONSABLE"})
    void unaAccionQueCreaOCambia_cuandoTraeNuevo_entoncesValeConOSinAnterior(Accion accion) {
        assertThat(cambio(accion, Map.of(), DESPUES).anterior()).isEmpty();
        assertThat(cambio(accion, ANTES, DESPUES).anterior()).isEqualTo(ANTES);
        assertThatThrownBy(() -> cambio(accion, ANTES, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
