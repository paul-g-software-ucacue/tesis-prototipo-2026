package ec.mdd.reporting.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * La suma de horas por componente, con la que se acumulan los totales institucionales (F20-H02).
 */
class HorasPorComponenteTest {

    @Test
    void dadasDosCargas_cuandoSeSuman_entoncesSumaCadaComponente() {
        HorasPorComponente una = horas("8", "2.5", "3", "0", "1");
        HorasPorComponente otra = horas("12", "0", "0", "4", "2.25");

        HorasPorComponente suma = una.mas(otra);

        assertThat(suma.clase()).isEqualByComparingTo("20");
        assertThat(suma.otrasDocencia()).isEqualByComparingTo("2.5");
        assertThat(suma.investigacion()).isEqualByComparingTo("3");
        assertThat(suma.vinculacion()).isEqualByComparingTo("4");
        assertThat(suma.gestion()).isEqualByComparingTo("3.25");
        assertThat(suma.total()).isEqualByComparingTo("32.75");
    }

    @Test
    void dadaNinguna_cuandoSeLeSuma_entoncesQuedaLaOtra() {
        HorasPorComponente otra = horas("8", "2.5", "3", "0", "1");

        assertThat(HorasPorComponente.NINGUNA.mas(otra)).isEqualTo(otra);
    }

    @Test
    void dadasHorasConDosDecimales_cuandoSeSuman_entoncesNoSeRedondean() {
        HorasPorComponente suma =
                horas("0.25", "0.33", "0", "0", "0").mas(horas("0.50", "0.33", "0", "0", "0"));

        assertThat(suma.clase()).isEqualByComparingTo("0.75");
        assertThat(suma.otrasDocencia()).isEqualByComparingTo("0.66");
        assertThat(suma.otrasDocencia().stripTrailingZeros().scale()).isLessThanOrEqualTo(2);
    }

    private static HorasPorComponente horas(
            String clase, String otras, String investigacion, String vinculacion, String gestion) {
        return new HorasPorComponente(
                new BigDecimal(clase),
                new BigDecimal(otras),
                new BigDecimal(investigacion),
                new BigDecimal(vinculacion),
                new BigDecimal(gestion));
    }
}
