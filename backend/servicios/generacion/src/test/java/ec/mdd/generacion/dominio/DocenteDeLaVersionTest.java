package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Un docente de una versión congelada con su dedicación y sus horas (F20-H02, D-117): las mismas de
 * su carga congelada, para que el reporte general y el individual digan lo mismo.
 */
class DocenteDeLaVersionTest {

    private static final CargaHoraria HORAS =
            new CargaHoraria(
                    new BigDecimal("11.25"),
                    new BigDecimal("2.5"),
                    new BigDecimal("3"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);

    @Test
    void dadaSuCargaCongelada_cuandoSeResume_entoncesTieneSuDedicacionYSusHoras() {
        CargaCongelada carga =
                new CargaCongelada(
                        "0109990010",
                        "PERALTA MALDONADO JUAN ANDRES",
                        Dedicacion.TC,
                        40,
                        List.of(
                                new AsignaturaCongelada(
                                        "BD-301",
                                        "Bases de datos",
                                        "A",
                                        "TDS",
                                        "Software",
                                        new BigDecimal("11.25"))),
                        List.of(
                                new ActividadCongelada(
                                        "140-b",
                                        "Tutorías",
                                        Componente.OTRAS_DOCENCIA,
                                        "TDS",
                                        null,
                                        null,
                                        new BigDecimal("2.5")),
                                new ActividadCongelada(
                                        "141-a",
                                        "Proyecto",
                                        Componente.INVESTIGACION,
                                        "TDS",
                                        "INV-01",
                                        "Suelos andinos",
                                        new BigDecimal("3"))),
                        HORAS);

        DocenteDeLaVersion docente = DocenteDeLaVersion.de(carga);

        assertThat(docente.identificacion()).isEqualTo("0109990010");
        assertThat(docente.nombre()).isEqualTo("PERALTA MALDONADO JUAN ANDRES");
        assertThat(docente.dedicacion()).contains(Dedicacion.TC);
        assertThat(docente.horas()).isEqualTo(HORAS);
    }

    @Test
    void dadoUnDocenteQueNoEstabaActivoAlCongelar_cuandoSeLee_entoncesNoTieneDedicacion() {
        DocenteDeLaVersion docente =
                new DocenteDeLaVersion("0109990020", "ORDOÑEZ VEGA MARIA JOSE", null, HORAS);

        assertThat(docente.dedicacion()).isEmpty();
    }

    @Test
    void dadoUnDocenteSinHoras_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(
                        () -> new DocenteDeLaVersion("0109990010", "PERALTA", Dedicacion.TC, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("horas");
    }
}
