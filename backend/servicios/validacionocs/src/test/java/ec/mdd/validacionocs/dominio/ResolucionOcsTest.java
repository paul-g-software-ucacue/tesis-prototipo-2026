package ec.mdd.validacionocs.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** El número de la resolución del OCS (F15-H01, FR-004). */
class ResolucionOcsTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 15);
    private static final String SHA = "a".repeat(64);

    private static ResolucionOcs con(String numero) {
        return new ResolucionOcs(numero, FECHA, SHA, "resolucion.pdf", 2048);
    }

    @Test
    void dadoUnNumeroConEspacios_cuandoSeCrea_entoncesSeRecorta() {
        assertThat(con("  RPC-SO-12-No.123-2026  ").numero()).isEqualTo("RPC-SO-12-No.123-2026");
    }

    @Test
    void dadoUnNumeroDe60_cuandoSeCrea_entoncesSeAdmite() {
        assertThat(con("x".repeat(60)).numero()).hasSize(60);
    }

    @Test
    void dadoUnNumeroVacioDeEspaciosODe61_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> con("")).isInstanceOf(NumeroDeResolucionInvalido.class);
        assertThatThrownBy(() -> con("   ")).isInstanceOf(NumeroDeResolucionInvalido.class);
        assertThatThrownBy(() -> con(null)).isInstanceOf(NumeroDeResolucionInvalido.class);
        assertThatThrownBy(() -> con("x".repeat(61)))
                .isInstanceOf(NumeroDeResolucionInvalido.class);
    }
}
