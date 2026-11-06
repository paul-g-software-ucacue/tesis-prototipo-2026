package ec.mdd.usuarios.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import ec.mdd.usuarios.aplicacion.EventoNoReconocido;
import ec.mdd.usuarios.puertos.entrada.EventoRecibido;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Roles entrega a Usuarios dentro del proceso identidad, sin HTTP (ADR-024). */
class ReceptorLocalDeUsuariosTest {

    private final List<EventoRecibido> recibidos = new ArrayList<>();
    private final ReceptorLocalDeUsuarios receptor = new ReceptorLocalDeUsuarios(recibidos::add);

    @Test
    void dadoElReceptorLocal_cuandoSePregunta_entoncesAtiendeAUsuarios() {
        assertThat(receptor.destino()).isEqualTo("usuarios");
    }

    @Test
    void dadoUnEventoDeRoles_cuandoLlega_entoncesLoPasaAlCasoDeUsoConElMismoSobreSinDestino() {
        EventoEntreProcesos evento =
                EventoEntreProcesos.nuevo(
                        "roles.EntradaBitacora",
                        "roles",
                        "usuarios",
                        Map.of("accion", "DESIGNAR_RESPONSABLE"),
                        Clock.systemUTC());

        receptor.recibir(evento);

        assertThat(recibidos)
                .singleElement()
                .satisfies(
                        recibido -> {
                            assertThat(recibido.idEvento()).isEqualTo(evento.idEvento());
                            assertThat(recibido.tipo()).isEqualTo("roles.EntradaBitacora");
                            assertThat(recibido.origen()).isEqualTo("roles");
                            assertThat(recibido.momento()).isEqualTo(evento.momento());
                            assertThat(recibido.contenido())
                                    .containsEntry("accion", "DESIGNAR_RESPONSABLE");
                        });
    }

    @Test
    void dadoUnRechazoDelCasoDeUso_cuandoLlega_entoncesLaExcepcionSale() {
        ReceptorLocalDeUsuarios rechaza =
                new ReceptorLocalDeUsuarios(
                        evento -> {
                            throw new EventoNoReconocido(evento.tipo());
                        });
        EventoEntreProcesos evento =
                EventoEntreProcesos.nuevo(
                        "roles.OtroEvento", "roles", "usuarios", Map.of(), Clock.systemUTC());

        assertThatThrownBy(() -> rechaza.recibir(evento)).isInstanceOf(EventoNoReconocido.class);
    }
}
