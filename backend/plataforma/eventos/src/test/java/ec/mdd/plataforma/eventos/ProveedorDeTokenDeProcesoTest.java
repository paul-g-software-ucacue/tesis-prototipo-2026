package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.plataforma.pruebas.ReceptorSimulado;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ProveedorDeTokenDeProcesoTest {

    private static final String CLAVE = "clave-del-proceso";

    private final ReceptorSimulado receptor = new ReceptorSimulado();

    @AfterEach
    void cerrar() {
        receptor.close();
    }

    @Test
    void dadoUnTokenVigente_cuandoSePideOtraVez_entoncesSeReutiliza() {
        ProveedorDeTokenDeProceso proveedor = proveedor(CLAVE, Clock.systemUTC());

        assertThat(proveedor.token()).isEqualTo("token-1");
        assertThat(proveedor.token()).isEqualTo("token-1");
        assertThat(receptor.tokensEmitidos()).isEqualTo(1);
    }

    @Test
    void dadoUnTokenQueVenceEnMenosDeUnMinuto_cuandoSePide_entoncesSeRenueva() {
        // El receptor emite tokens de 5 minutos; con el reloj 4 min 30 s adelante, le quedan 30 s.
        Clock adelantado = Clock.offset(Clock.systemUTC(), Duration.ofSeconds(270));
        ProveedorDeTokenDeProceso proveedor = proveedor(CLAVE, adelantado);

        assertThat(proveedor.token()).isEqualTo("token-1");
        assertThat(proveedor.token()).isEqualTo("token-2");
    }

    @Test
    void dadoUnTokenDescartado_cuandoSePide_entoncesSePideOtro() {
        ProveedorDeTokenDeProceso proveedor = proveedor(CLAVE, Clock.systemUTC());
        proveedor.token();

        proveedor.descartar();

        assertThat(proveedor.token()).isEqualTo("token-2");
    }

    @Test
    void dadoQueUsuariosNoEmiteElToken_cuandoSePide_entoncesEntregaFallidaSinLaClave(
            CapturedOutput salida) {
        receptor.responderToken(503);

        assertThatThrownBy(() -> proveedor(CLAVE, Clock.systemUTC()).token())
                .isInstanceOf(EntregaFallida.class)
                .hasMessageContaining("503")
                .message()
                .doesNotContain(CLAVE);
        assertThat(salida.getAll()).doesNotContain(CLAVE).doesNotContain("token-");
    }

    @Test
    void dadoUsuariosDetenido_cuandoSePideElToken_entoncesEntregaFallida() {
        ProveedorDeTokenDeProceso proveedor = proveedor(CLAVE, Clock.systemUTC());
        receptor.detener();

        assertThatThrownBy(proveedor::token)
                .isInstanceOf(EntregaFallida.class)
                .hasMessageContaining("token de proceso");
        receptor.levantar();
    }

    @Test
    void dadoLaClaveVacia_cuandoSePideElToken_entoncesFallaSinLlamarAUsuarios() {
        assertThatThrownBy(() -> proveedor(" ", Clock.systemUTC()).token())
                .isInstanceOf(EntregaFallida.class)
                .hasMessageContaining("sin clave de proceso");
        assertThat(receptor.tokensEmitidos()).isZero();
    }

    @Test
    void dadoLaFechaDeVencimiento_cuandoSeCalculaSiSirve_entoncesUsaElMargenDeUnMinuto() {
        Instant ahora = Instant.parse("2026-10-14T12:00:00Z");
        Clock reloj = Clock.fixed(ahora, ZoneOffset.UTC);

        assertThat(ProveedorDeTokenDeProceso.sirve(ahora.plusSeconds(61), reloj)).isTrue();
        assertThat(ProveedorDeTokenDeProceso.sirve(ahora.plusSeconds(60), reloj)).isFalse();
    }

    private ProveedorDeTokenDeProceso proveedor(String clave, Clock reloj) {
        return new ProveedorDeTokenDeProceso(
                Clientes.http(Duration.ofSeconds(2)),
                receptor.urlToken(),
                "distributivo",
                clave,
                reloj);
    }
}
