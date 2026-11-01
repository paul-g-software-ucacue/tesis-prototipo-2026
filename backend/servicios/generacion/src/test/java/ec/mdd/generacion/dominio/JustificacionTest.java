package ec.mdd.generacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** La justificación de una propuesta la decide el dominio (RN-19, RNF-EQU-01). */
class JustificacionTest {

    private static final BigDecimal MINIMO = new BigDecimal("3.5");
    private static final String TEXTO = "No hay otro docente disponible este periodo.";

    @Test
    void dadoQuienAlcanzaElMinimo_cuandoSeDecide_entoncesEsAfinidadSinTexto() {
        Justificacion justificacion = Justificacion.para(new BigDecimal("7"), MINIMO, false, null);

        assertThat(justificacion.tipo()).isEqualTo(TipoDeJustificacion.AFINIDAD);
        assertThat(justificacion.texto()).isNull();
    }

    @Test
    void dadoUnPuntajeIgualAlMinimoConOtraEscala_cuandoSeDecide_entoncesEsAfinidad() {
        assertThat(Justificacion.para(new BigDecimal("3.50"), MINIMO, false, null).tipo())
                .isEqualTo(TipoDeJustificacion.AFINIDAD);
    }

    @Test
    void dadoQuienAlcanzaElMinimoEnTroncoComun_cuandoSeDecide_entoncesEsAfinidad() {
        assertThat(Justificacion.para(new BigDecimal("7"), MINIMO, true, null).tipo())
                .isEqualTo(TipoDeJustificacion.AFINIDAD);
    }

    @Test
    void dadoQuienNoLoAlcanzaEnTroncoComun_cuandoSeDecide_entoncesEsDisponibilidadSinTexto() {
        Justificacion justificacion = Justificacion.para(BigDecimal.ZERO, MINIMO, true, null);

        assertThat(justificacion.tipo()).isEqualTo(TipoDeJustificacion.DISPONIBILIDAD);
        assertThat(justificacion.texto()).isNull();
    }

    @Test
    void dadoQuienNoLoAlcanzaSinTroncoComun_cuandoSeDecide_entoncesEsExcepcionConSuTexto() {
        Justificacion justificacion =
                Justificacion.para(BigDecimal.ONE, MINIMO, false, "  " + TEXTO + "  ");

        assertThat(justificacion.tipo()).isEqualTo(TipoDeJustificacion.EXCEPCION);
        assertThat(justificacion.texto()).isEqualTo(TEXTO);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "          ", "123456789"})
    void dadaUnaExcepcionSinTextoSuficiente_cuandoSeDecide_entoncesLoExige(String texto) {
        assertThatThrownBy(() -> Justificacion.para(BigDecimal.ONE, MINIMO, false, texto))
                .isInstanceOf(JustificacionObligatoria.class);
    }

    @Test
    void dadosLosLimitesDelTexto_cuandoSeDecide_entoncesDiezYQuinientosPasanYMasNo() {
        assertThat(Justificacion.para(BigDecimal.ONE, MINIMO, false, "1234567890").texto())
                .hasSize(10);
        assertThat(Justificacion.para(BigDecimal.ONE, MINIMO, false, "x".repeat(500)).texto())
                .hasSize(500);
        assertThatThrownBy(() -> Justificacion.para(BigDecimal.ONE, MINIMO, false, "x".repeat(501)))
                .isInstanceOf(JustificacionObligatoria.class);
    }

    @Test
    void dadoUnTextoQueNoHaceFalta_cuandoSeDecide_entoncesNoSeConserva() {
        assertThat(Justificacion.para(new BigDecimal("7"), MINIMO, false, TEXTO).texto()).isNull();
        assertThat(Justificacion.para(BigDecimal.ZERO, MINIMO, true, TEXTO).texto()).isNull();
    }
}
