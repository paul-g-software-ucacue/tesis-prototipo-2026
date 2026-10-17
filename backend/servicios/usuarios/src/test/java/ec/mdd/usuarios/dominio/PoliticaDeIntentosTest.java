package ec.mdd.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Regla del bloqueo por intentos fallidos (D-60), sin dobles y con instantes fijos. */
class PoliticaDeIntentosTest {

    private static final Instant AHORA = Instant.parse("2026-10-17T15:00:00Z");
    private final PoliticaDeIntentos politica = new PoliticaDeIntentos(5, Duration.ofMinutes(15));

    @Test
    void dadoDeUnoACuatroFallos_cuandoSeEvalua_entoncesNoBloquea() {
        for (int fallidos = 1; fallidos <= 4; fallidos++) {
            assertThat(politica.alcanzaElMaximo(fallidos)).isFalse();
        }
    }

    @Test
    void dadoElQuintoFallo_cuandoSeEvalua_entoncesBloqueaQuinceMinutos() {
        assertThat(politica.alcanzaElMaximo(5)).isTrue();
        assertThat(politica.bloqueadoHasta(AHORA)).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
    }

    @Test
    void dadoUnBloqueoVigente_cuandoSeConsulta_entoncesEstaBloqueadoYDiceCuantoFalta() {
        EstadoDeIntentos estado = new EstadoDeIntentos(5, AHORA.plusSeconds(600));

        assertThat(politica.estaBloqueado(estado, AHORA)).isTrue();
        assertThat(politica.segundosRestantes(estado, AHORA)).isEqualTo(600);
        assertThat(politica.segundosRestantes(estado, AHORA.plusMillis(599_500))).isEqualTo(1);
    }

    @Test
    void dadoUnBloqueoVencido_cuandoSeConsulta_entoncesNoEstaBloqueadoYLaCuentaVuelveAEmpezar() {
        EstadoDeIntentos estado = new EstadoDeIntentos(5, AHORA.minusSeconds(1));

        assertThat(politica.estaBloqueado(estado, AHORA)).isFalse();
        assertThat(politica.bloqueoVencido(estado, AHORA)).isTrue();
        assertThat(politica.bloqueoVencido(EstadoDeIntentos.SIN_INTENTOS, AHORA)).isFalse();
    }

    @Test
    void dadoSinIntentos_cuandoSeConsulta_entoncesNoEstaBloqueado() {
        assertThat(politica.estaBloqueado(EstadoDeIntentos.SIN_INTENTOS, AHORA)).isFalse();
        assertThat(politica.estaBloqueado(new EstadoDeIntentos(4, null), AHORA)).isFalse();
    }

    @Test
    void dadoOtrosParametros_cuandoSeEvalua_entoncesLosRespeta() {
        PoliticaDeIntentos estricta = new PoliticaDeIntentos(3, Duration.ofMinutes(1));

        assertThat(estricta.alcanzaElMaximo(3)).isTrue();
        assertThat(estricta.bloqueadoHasta(AHORA)).isEqualTo(AHORA.plusSeconds(60));
    }

    @Test
    void dadoParametrosSinSentido_cuandoSeCrea_entoncesLosRechaza() {
        assertThatThrownBy(() -> new PoliticaDeIntentos(0, Duration.ofMinutes(15)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PoliticaDeIntentos(5, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
