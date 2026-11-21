package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** El contenido congelado de un docente en una versión (F20-H01, D-115). */
class CargaCongeladaTest {

    private static final AsignaturaCongelada BD_A =
            new AsignaturaCongelada("BD-301", "Bases de datos", "A", "TDS", "Software", horas("8"));
    private static final AsignaturaCongelada SOC_B =
            new AsignaturaCongelada(
                    "SOC-101", "Sociología", "B", "AGRO", "Agronomía", horas("3.25"));
    private static final ActividadCongelada TUTORIA =
            new ActividadCongelada(
                    "140-b",
                    "Tutorías",
                    Componente.OTRAS_DOCENCIA,
                    "TDS",
                    null,
                    null,
                    horas("2.5"));
    private static final ActividadCongelada PROYECTO =
            new ActividadCongelada(
                    "141-a",
                    "Proyecto de investigación",
                    Componente.INVESTIGACION,
                    "TDS",
                    "INV-01",
                    "Suelos andinos",
                    horas("3"));

    @Test
    void dadasSusAsignaciones_cuandoSeCongela_entoncesLasOrdenaPorCarreraYComponente() {
        CargaCongelada carga =
                new CargaCongelada(
                        "0109990010",
                        "Pérez Ana",
                        Dedicacion.TC,
                        40,
                        List.of(BD_A, SOC_B),
                        List.of(PROYECTO, TUTORIA),
                        new CargaHoraria(
                                horas("11.25"),
                                horas("2.5"),
                                horas("3"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO));

        assertThat(carga.asignaturas()).containsExactly(SOC_B, BD_A);
        assertThat(carga.actividades()).containsExactly(TUTORIA, PROYECTO);
        assertThat(carga.dedicacion()).contains(Dedicacion.TC);
        assertThat(carga.horasContratadas()).hasValue(40);
    }

    @Test
    void dadoUnDocenteQueYaNoEstabaActivo_cuandoSeCongela_entoncesNoTieneDedicacion() {
        CargaCongelada carga =
                new CargaCongelada(
                        "0109990010",
                        "Pérez Ana",
                        null,
                        null,
                        List.of(),
                        List.of(TUTORIA),
                        new CargaHoraria(
                                BigDecimal.ZERO,
                                horas("2.5"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO));

        assertThat(carga.dedicacion()).isEmpty();
        assertThat(carga.horasContratadas()).isEmpty();
    }

    @Test
    void dadasClasesQueNoSumanSuCarga_cuandoSeCongela_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                new CargaCongelada(
                                        "0109990010",
                                        "Pérez Ana",
                                        Dedicacion.TC,
                                        40,
                                        List.of(BD_A),
                                        List.of(),
                                        new CargaHoraria(
                                                horas("9"),
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CLASE");
    }

    @Test
    void dadasActividadesQueNoSumanSuComponente_cuandoSeCongela_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                new CargaCongelada(
                                        "0109990010",
                                        "Pérez Ana",
                                        Dedicacion.TC,
                                        40,
                                        List.of(),
                                        List.of(TUTORIA),
                                        new CargaHoraria(
                                                BigDecimal.ZERO,
                                                horas("3"),
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OTRAS_DOCENCIA");
    }

    @Test
    void dadasHorasContratadasFueraDeRango_cuandoSeCongela_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                new CargaCongelada(
                                        "0109990010",
                                        "Pérez Ana",
                                        Dedicacion.TC,
                                        41,
                                        List.of(),
                                        List.of(),
                                        new CargaHoraria(
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static BigDecimal horas(String valor) {
        return new BigDecimal(valor);
    }
}
