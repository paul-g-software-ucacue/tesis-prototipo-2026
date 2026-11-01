package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** La suma de las horas de actividades por componente, sin dobles. */
class SumaPorComponenteTest {

    @Test
    void dadasActividades_cuandoSeSuman_entoncesVanPorComponenteConDecimales() {
        SumaPorComponente suma =
                SumaPorComponente.de(
                        List.of(
                                una(Componente.OTRAS_DOCENCIA, "4.25"),
                                una(Componente.OTRAS_DOCENCIA, "2.5"),
                                una(Componente.INVESTIGACION, "6"),
                                una(Componente.VINCULACION, "1.75"),
                                una(Componente.GESTION, "3")));

        assertThat(suma.otrasDocencia()).isEqualByComparingTo("6.75");
        assertThat(suma.investigacion()).isEqualByComparingTo("6");
        assertThat(suma.vinculacion()).isEqualByComparingTo("1.75");
        assertThat(suma.gestion()).isEqualByComparingTo("3");
    }

    @Test
    void dadoQueNoHayActividades_cuandoSeSuman_entoncesTodoEsCero() {
        SumaPorComponente suma = SumaPorComponente.de(List.of());

        assertThat(suma.otrasDocencia()).isEqualByComparingTo("0");
        assertThat(suma.gestion()).isEqualByComparingTo("0");
    }

    private static ActividadNoLectiva una(Componente componente, String horas) {
        return new ActividadNoLectiva(
                "TDS",
                "0109990010",
                "PEREZ LUIS",
                "140-b",
                "Planificar",
                componente,
                null,
                null,
                new BigDecimal(horas),
                Instant.parse("2026-11-01T15:00:00Z"),
                "coordinacion.demo");
    }
}
