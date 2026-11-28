package ec.mdd.reporting.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** La neutralización de las fórmulas en los CSV (RNF-SEG-04), común a los reportes y a SIGA. */
class CeldaCsvTest {

    @ParameterizedTest
    @ValueSource(strings = {"=1+1", "+57", "-3", "@SUMA", "\tA", "\rA"})
    void dadoUnValorQueEmpiezaComoFormula_cuandoSeNeutraliza_entoncesLlevaUnApostrofo(
            String peligroso) {
        assertThat(CeldaCsv.neutralizada(peligroso)).isEqualTo("'" + peligroso);
    }

    @Test
    void dadoUnValorNormalOVacio_cuandoSeNeutraliza_entoncesNoCambia() {
        assertThat(CeldaCsv.neutralizada("PEÑA ÁLVAREZ JOSÉ")).isEqualTo("PEÑA ÁLVAREZ JOSÉ");
        assertThat(CeldaCsv.neutralizada("140-B")).isEqualTo("140-B");
        assertThat(CeldaCsv.neutralizada("")).isEmpty();
    }
}
