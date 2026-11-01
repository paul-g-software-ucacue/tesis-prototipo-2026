package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Las horas de una actividad (FR-006, D-77), sin dobles. */
class ActividadNoLectivaTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "4", "4.5", "4.25", "4.500", "40", "40.00"})
    void dadasHorasValidas_cuandoSeCrea_entoncesLasAcepta(String horas) {
        assertThat(actividad(horas, null).horas()).isEqualByComparingTo(horas);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "40.01", "41", "4.125", "0.001"})
    void dadasHorasNoValidas_cuandoSeCrea_entoncesLasRechaza(String horas) {
        assertThatThrownBy(() -> actividad(horas, null)).isInstanceOf(HorasNoValidas.class);
    }

    @Test
    void dadaUnaActividad_cuandoSeCompara_entoncesLaLlaveEsCarreraDocenteActividadYProyecto() {
        ActividadNoLectiva una = actividad("4", "INV-2025-01");

        assertThat(una.esDe("TDS", "0109990010", "141-a", "INV-2025-01")).isTrue();
        assertThat(una.esDe("TDS", "0109990010", "141-a", null)).isFalse();
        assertThat(una.esDe("TDS", "0109990010", "141-a", "INV-2025-02")).isFalse();
        assertThat(una.esDe("TDS", "0109990010", "141-b", "INV-2025-01")).isFalse();
        assertThat(una.esDe("TDS", "0109990020", "141-a", "INV-2025-01")).isFalse();
        assertThat(una.esDe("CONT", "0109990010", "141-a", "INV-2025-01")).isFalse();
    }

    private static ActividadNoLectiva actividad(String horas, String proyecto) {
        return new ActividadNoLectiva(
                "TDS",
                "0109990010",
                "PEREZ LUIS",
                "141-a",
                "Investigar",
                Componente.INVESTIGACION,
                proyecto,
                proyecto == null ? null : "Un proyecto",
                new BigDecimal(horas),
                Instant.parse("2026-11-01T15:00:00Z"),
                "coordinacion.demo");
    }
}
