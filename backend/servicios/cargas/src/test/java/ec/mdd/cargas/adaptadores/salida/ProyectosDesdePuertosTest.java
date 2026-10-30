package ec.mdd.cargas.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.cargas.puertos.salida.PeriodoNoEncontrado;
import ec.mdd.docentes.puertos.entrada.AsignablesDelPeriodo;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** La consulta de proyectos asignables de Docentes, con un doble escrito a mano (F01-H05). */
class ProyectosDesdePuertosTest {

    private final ProyectosDesdePuertos proyectos =
            new ProyectosDesdePuertos(
                    periodo ->
                            Map.of(
                                            "2026-I", AsignablesDelPeriodo.ALGUNO,
                                            "2026-II", AsignablesDelPeriodo.NINGUNO)
                                    .getOrDefault(
                                            periodo, AsignablesDelPeriodo.PERIODO_NO_ENCONTRADO));

    @Test
    void unPeriodoConAlgunoAsignable_tieneAsignables_yUnoSinNinguno_no() {
        assertThat(proyectos.hayAsignables("2026-I")).isTrue();
        assertThat(proyectos.hayAsignables("2026-II")).isFalse();
    }

    @Test
    void unPeriodoQueNoEstaEnAcademico_esPeriodoNoEncontradoConSuCodigo() {
        assertThatThrownBy(() -> proyectos.hayAsignables("2027-I"))
                .isInstanceOfSatisfying(
                        PeriodoNoEncontrado.class,
                        error -> assertThat(error.periodo()).isEqualTo("2027-I"));
    }
}
