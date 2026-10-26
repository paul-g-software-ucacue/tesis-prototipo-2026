package ec.mdd.academico.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Los matriculados de una carrera en un periodo (carga 10), sin dobles. */
class MatriculaTest {

    private static final LocalDate CORTE = LocalDate.of(2026, 4, 30);

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = {0, 1, 99_999})
    void unTotalDe0a99999Pasa(int total) {
        assertThat(new Matricula("2026-I", "C1", total, CORTE, "SIGA").total()).isEqualTo(total);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = {-1, 100_000})
    void unTotalFueraDeRangoNoPasa(int total) {
        assertThatThrownBy(() -> new Matricula("2026-I", "C1", total, CORTE, null))
                .isInstanceOfSatisfying(
                        DatoInvalido.class, error -> assertThat(error.campo()).isEqualTo("total"));
    }

    @Test
    void laFuenteVaHasta60YLasReferenciasSonObligatorias() {
        assertThat(new Matricula("2026-I", "C1", 5, CORTE, "F".repeat(60)).fuente()).hasSize(60);
        assertThatThrownBy(() -> new Matricula("2026-I", "C1", 5, CORTE, "F".repeat(61)))
                .isInstanceOf(DatoInvalido.class);
        assertThatThrownBy(() -> new Matricula("", "C1", 5, CORTE, null))
                .isInstanceOf(DatoInvalido.class);
        assertThatThrownBy(() -> new Matricula("2026-I", " ", 5, CORTE, null))
                .isInstanceOf(DatoInvalido.class);
    }
}
