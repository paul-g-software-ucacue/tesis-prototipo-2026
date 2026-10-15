package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.plataforma.pruebas.ReceptorSimulado;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class OyenteEntreProcesosTest {

    private final ReceptorSimulado receptor = new ReceptorSimulado();
    private final OyenteEntreProcesos oyente =
            new OyenteEntreProcesos(
                    new ClienteDeEventos(
                            Clientes.http(Duration.ofSeconds(2)),
                            Map.of("usuarios", receptor.urlEventos()),
                            new ProveedorDeTokenDeProceso(
                                    Clientes.http(Duration.ofSeconds(2)),
                                    receptor.urlToken(),
                                    "distributivo",
                                    "clave",
                                    Clock.systemUTC())));
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
    void dadoElReceptorQueConfirma_cuandoSeEntrega_entoncesTermina() {
        assertThatCode(() -> oyente.entregar(evento)).doesNotThrowAnyException();
        assertThat(receptor.eventosRecibidos()).hasSize(1);
    }

    @Test
    void dadoUnaEntregaFallida_cuandoSeEntrega_entoncesRegistraSinElContenidoYVuelveALanzar(
            CapturedOutput salida) {
        receptor.responderEventos(500);

        assertThatThrownBy(() -> oyente.entregar(evento)).isInstanceOf(EntregaFallida.class);
        assertThat(salida.getAll())
                .contains("Entrega fallida del evento " + evento.idEvento())
                .contains("generacion.EventoDePrueba")
                .contains("usuarios")
                .contains("500")
                .doesNotContain("contenido-que-no-se-registra")
                .doesNotContain("token-1");
    }
}
