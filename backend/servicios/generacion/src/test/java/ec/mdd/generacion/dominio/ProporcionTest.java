package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** La proporción de clase y otras actividades de docencia: enteros que suman 100 (RN-05). */
class ProporcionTest {

    @ParameterizedTest
    @CsvSource({
        "8, 4, 67, 33",
        "1, 2, 33, 67",
        "12, 8, 60, 40",
        "1, 7, 13, 87",
        "2.25, 0.75, 75, 25",
        "1, 199, 1, 99",
    })
    void dadasUnasHoras_cuandoSeCalcula_entoncesLosPorcentajesSonEnteros(
            String clase, String otras, int deClase, int deOtras) {
        Proporcion proporcion =
                Proporcion.de(new BigDecimal(clase), new BigDecimal(otras)).orElseThrow();

        assertThat(proporcion.clase()).isEqualTo(deClase);
        assertThat(proporcion.otrasDocencia()).isEqualTo(deOtras);
    }

    @Test
    void dadoUnMedioExacto_cuandoSeRedondea_entoncesVaHaciaArriba() {
        Proporcion proporcion =
                Proporcion.de(new BigDecimal("1"), new BigDecimal("199")).orElseThrow();

        assertThat(proporcion.clase()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"8, 4", "1, 2", "1, 3", "2, 3", "5.55, 3.33", "0.01, 39.99", "13, 7"})
    void dadasCualesquieraHoras_cuandoSeCalcula_entoncesSuman100(String clase, String otras) {
        Proporcion proporcion =
                Proporcion.de(new BigDecimal(clase), new BigDecimal(otras)).orElseThrow();

        assertThat(proporcion.clase() + proporcion.otrasDocencia()).isEqualTo(100);
    }

    @Test
    void dadaSoloClase_cuandoSeCalcula_entoncesEs100Y0() {
        Proporcion proporcion = Proporcion.de(new BigDecimal("6"), BigDecimal.ZERO).orElseThrow();

        assertThat(proporcion.clase()).isEqualTo(100);
        assertThat(proporcion.otrasDocencia()).isZero();
    }

    @Test
    void dadasSoloOtras_cuandoSeCalcula_entoncesEs0Y100() {
        Proporcion proporcion = Proporcion.de(BigDecimal.ZERO, new BigDecimal("6")).orElseThrow();

        assertThat(proporcion.clase()).isZero();
        assertThat(proporcion.otrasDocencia()).isEqualTo(100);
    }

    @Test
    void sinHorasEnNinguna_cuandoSeCalcula_entoncesNoHayProporcion() {
        assertThat(Proporcion.de(BigDecimal.ZERO, BigDecimal.ZERO)).isEmpty();
    }
}
