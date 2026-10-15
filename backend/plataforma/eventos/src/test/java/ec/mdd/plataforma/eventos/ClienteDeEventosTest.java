package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.plataforma.pruebas.ReceptorSimulado;
import ec.mdd.plataforma.pruebas.ValidadorDeContrato;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ClienteDeEventosTest {

    private final ReceptorSimulado receptor = new ReceptorSimulado();
    private final EventoEntreProcesos evento =
            EventoEntreProcesos.nuevo(
                    "generacion.EventoDePrueba",
                    "generacion",
                    "usuarios",
                    Map.of("nota", "contenido-que-no-se-registra"),
                    Clock.systemUTC());

    @AfterEach
    void cerrar() {
        receptor.close();
    }

    @Test
    void dadoElReceptorQueConfirma_cuandoSeEntrega_entoncesEnviaElCuerpoSinDestinoConElToken() {
        assertThatCode(() -> cliente("clave").entregar(evento)).doesNotThrowAnyException();

        assertThat(receptor.eventosRecibidos()).hasSize(1);
        ReceptorSimulado.Peticion peticion = receptor.eventosRecibidos().getFirst();
        assertThat(peticion.autorizacion()).isEqualTo("Bearer token-1");
        assertThat(peticion.cuerpo())
                .contains(evento.idEvento().toString())
                .doesNotContain("destino");
        ValidadorDeContrato.para("usuarios")
                .validarPeticion("POST", "/interno/usuarios/eventos", peticion.cuerpo());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 500, 503})
    void dadoUnaRespuestaDeError_cuandoSeEntrega_entoncesEntregaFallidaConElEstado(int estado) {
        receptor.responderEventos(estado);

        assertThatThrownBy(() -> cliente("clave").entregar(evento))
                .isInstanceOf(EntregaFallida.class)
                .hasMessageContaining(String.valueOf(estado));
    }

    @Test
    void dadoUnReceptorQueNoRespondeEnElPlazo_cuandoSeEntrega_entoncesEntregaFallida() {
        receptor.retrasar(Duration.ofSeconds(3));

        assertThatThrownBy(() -> cliente("clave", Duration.ofMillis(500)).entregar(evento))
                .isInstanceOf(EntregaFallida.class);
        receptor.retrasar(Duration.ZERO);
    }

    @Test
    void dadoElReceptorDetenido_cuandoSeEntrega_entoncesEntregaFallida() {
        ClienteDeEventos cliente = cliente("clave");
        cliente.entregar(evento);
        receptor.detener();

        assertThatThrownBy(() -> cliente.entregar(evento)).isInstanceOf(EntregaFallida.class);
        receptor.levantar();
    }

    @Test
    void dadoUnHostDesconocido_cuandoSeEntrega_entoncesEntregaFallida() {
        ClienteDeEventos cliente =
                new ClienteDeEventos(
                        Clientes.http(Duration.ofSeconds(1)),
                        Map.of("usuarios", URI.create("http://host-que-no-existe.invalid/x")),
                        proveedor("clave"));

        assertThatThrownBy(() -> cliente.entregar(evento)).isInstanceOf(EntregaFallida.class);
    }

    @Test
    void dadoUnTokenRechazado_cuandoSeEntrega_entoncesPideOtroYReintentaUnaVez() {
        receptor.responderEventos(401);

        assertThatCode(() -> cliente("clave").entregar(evento)).doesNotThrowAnyException();

        assertThat(receptor.eventosRecibidos())
                .extracting(ReceptorSimulado.Peticion::autorizacion)
                .containsExactly("Bearer token-1", "Bearer token-2");
    }

    @Test
    void dadoDosRechazosDelToken_cuandoSeEntrega_entoncesEntregaFallida() {
        receptor.responderEventos(401, 401);

        assertThatThrownBy(() -> cliente("clave").entregar(evento))
                .isInstanceOf(EntregaFallida.class)
                .hasMessageContaining("401");
        assertThat(receptor.eventosRecibidos()).hasSize(2);
    }

    @Test
    void dadoUnDestinoSinUrl_cuandoSeEntrega_entoncesEntregaFallida() {
        EventoEntreProcesos otro =
                EventoEntreProcesos.nuevo(
                        "generacion.EventoDePrueba",
                        "generacion",
                        "notificaciones",
                        Map.of(),
                        Clock.systemUTC());

        assertThatThrownBy(() -> cliente("clave").entregar(otro))
                .isInstanceOf(EntregaFallida.class)
                .hasMessageContaining("notificaciones");
    }

    @Test
    void dadoLaClaveVacia_cuandoSeEntrega_entoncesNoPideElTokenYEntregaFallida(
            CapturedOutput salida) {
        assertThatThrownBy(() -> cliente("").entregar(evento))
                .isInstanceOf(EntregaFallida.class)
                .hasMessageContaining("sin clave de proceso");
        assertThat(receptor.tokensEmitidos()).isZero();
        assertThat(receptor.eventosRecibidos()).isEmpty();
        assertThat(salida.getAll()).doesNotContain("contenido-que-no-se-registra");
    }

    private ClienteDeEventos cliente(String clave) {
        return cliente(clave, Duration.ofSeconds(2));
    }

    private ClienteDeEventos cliente(String clave, Duration plazo) {
        return new ClienteDeEventos(
                Clientes.http(plazo), Map.of("usuarios", receptor.urlEventos()), proveedor(clave));
    }

    private ProveedorDeTokenDeProceso proveedor(String clave) {
        return new ProveedorDeTokenDeProceso(
                Clientes.http(Duration.ofSeconds(2)),
                receptor.urlToken(),
                "distributivo",
                clave,
                Clock.systemUTC());
    }
}
