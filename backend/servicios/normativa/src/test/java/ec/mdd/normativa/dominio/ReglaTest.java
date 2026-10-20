package ec.mdd.normativa.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Una regla tiene parámetros, severidad o las dos (research §2 de F04-H03), sin dobles. */
class ReglaTest {

    @Test
    void dadoRf24ConSeveridadYSinParametros_cuandoSeCrea_entoncesEsDelMotor() {
        Regla regla = new Regla("RF-24", Severidad.DURA, Map.of());

        assertThat(regla.parametros()).isEmpty();
        assertThat(regla.delMotor()).isTrue();
    }

    @Test
    void dadoUnaReglaDelMotorSinCifras_cuandoSeCrea_entoncesSoloTieneSuSeveridad() {
        Regla regla = new Regla("RN-17", Severidad.DURA, Map.of());

        assertThat(regla.severidad()).isEqualTo(Severidad.DURA);
        assertThat(regla.delMotor()).isTrue();
    }

    @Test
    void dadoRn24ConParametrosYSinSeveridad_cuandoSeCrea_entoncesNoEsDelMotor() {
        Regla regla = new Regla("RN-24", null, Map.of("tope_cursos", new BigDecimal("3")));

        assertThat(regla.parametro("tope_cursos")).contains(new BigDecimal("3"));
        assertThat(regla.delMotor()).isFalse();
    }

    @Test
    void dadoUnaReglaSinParametrosNiSeveridad_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> new Regla("RN-17", null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RN-17");
    }

    @Test
    void dadoUnIdentificadorMalFormado_cuandoSeCrea_entoncesSeRechaza() {
        for (String identificador : new String[] {"RX-01", "RN-1", "rn-02", "RF-024"}) {
            assertThatThrownBy(() -> new Regla(identificador, Severidad.DURA, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(identificador);
        }
    }
}
