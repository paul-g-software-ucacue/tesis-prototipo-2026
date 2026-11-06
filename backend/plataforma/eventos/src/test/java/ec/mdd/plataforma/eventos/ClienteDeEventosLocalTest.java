package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.plataforma.pruebas.ReceptorSimulado;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** La entrega a un receptor del mismo proceso, sin HTTP ni token (ADR-024). */
class ClienteDeEventosLocalTest {

    private final ReceptorSimulado remoto = new ReceptorSimulado();
    private final EventoEntreProcesos evento =
            EventoEntreProcesos.nuevo(
                    "roles.EntradaBitacora",
                    "roles",
                    "usuarios",
                    Map.of("nota", "contenido-que-no-se-registra"),
                    Clock.systemUTC());

    @AfterEach
    void cerrar() {
        remoto.close();
    }

    @Test
    void dadoUnReceptorLocalDelDestino_cuandoSeEntrega_entoncesLoLlamaSinHttpNiToken() {
        Anotador local = new Anotador("usuarios");

        assertThatCode(() -> cliente(List.of(local)).entregar(evento)).doesNotThrowAnyException();

        assertThat(local.recibidos).containsExactly(evento);
        assertThat(remoto.eventosRecibidos()).isEmpty();
        assertThat(remoto.tokensEmitidos()).isZero();
    }

    @Test
    void dadoUnReceptorLocalQueFalla_cuandoSeEntrega_entoncesEntregaFallidaSinElContenido() {
        ReceptorLocal falla =
                new ReceptorLocal() {
                    @Override
                    public String destino() {
                        return "usuarios";
                    }

                    @Override
                    public void recibir(EventoEntreProcesos recibido) {
                        throw new IllegalStateException(
                                "detalle interno " + recibido.contenido().get("nota"));
                    }
                };

        assertThatThrownBy(() -> cliente(List.of(falla)).entregar(evento))
                .isInstanceOf(EntregaFallida.class)
                .hasMessageContaining("IllegalStateException")
                .message()
                .doesNotContain("contenido-que-no-se-registra")
                .doesNotContain("detalle interno");
    }

    @Test
    void dadoUnReceptorLocalDeOtroDestino_cuandoSeEntrega_entoncesEntregaPorHttp() {
        Anotador otro = new Anotador("notificaciones");

        assertThatCode(() -> cliente(List.of(otro)).entregar(evento)).doesNotThrowAnyException();

        assertThat(otro.recibidos).isEmpty();
        assertThat(remoto.eventosRecibidos()).hasSize(1);
    }

    @Test
    void dadoDosReceptoresLocalesDelMismoDestino_cuandoSeArma_entoncesSeRechaza() {
        List<ReceptorLocal> dos = List.of(new Anotador("usuarios"), new Anotador("usuarios"));

        assertThatThrownBy(() -> cliente(dos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("usuarios");
    }

    @Test
    void dadoSinUrlNiReceptorLocal_cuandoSeEntrega_entoncesEntregaFallida() {
        ClienteDeEventos cliente =
                new ClienteDeEventos(
                        Clientes.http(Duration.ofSeconds(1)), Map.of(), proveedor(), List.of());

        assertThatThrownBy(() -> cliente.entregar(evento))
                .isInstanceOf(EntregaFallida.class)
                .hasMessageContaining("usuarios");
    }

    private ClienteDeEventos cliente(List<ReceptorLocal> locales) {
        return new ClienteDeEventos(
                Clientes.http(Duration.ofSeconds(2)),
                Map.of("usuarios", remoto.urlEventos()),
                proveedor(),
                locales);
    }

    private ProveedorDeTokenDeProceso proveedor() {
        return new ProveedorDeTokenDeProceso(
                Clientes.http(Duration.ofSeconds(2)),
                remoto.urlToken(),
                "identidad",
                "clave",
                Clock.systemUTC());
    }

    /** Un receptor local que anota lo que recibe. */
    private static final class Anotador implements ReceptorLocal {
        private final String destino;
        private final List<EventoEntreProcesos> recibidos = new ArrayList<>();

        Anotador(String destino) {
            this.destino = destino;
        }

        @Override
        public String destino() {
            return destino;
        }

        @Override
        public void recibir(EventoEntreProcesos recibido) {
            recibidos.add(recibido);
        }
    }
}
