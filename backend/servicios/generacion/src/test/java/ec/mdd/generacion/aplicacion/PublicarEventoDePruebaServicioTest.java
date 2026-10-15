package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ec.mdd.generacion.puertos.salida.EventoSaliente;
import ec.mdd.generacion.puertos.salida.PublicadorDeEventos;
import ec.mdd.generacion.puertos.salida.RepositorioEventosDePrueba;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** El evento de prueba con los puertos de salida simulados (principio VIII: Mockito solo aquí). */
class PublicarEventoDePruebaServicioTest {

    private final RepositorioEventosDePrueba repositorio = mock(RepositorioEventosDePrueba.class);
    private final PublicadorDeEventos publicador = mock(PublicadorDeEventos.class);
    private final Instant ahora = Instant.parse("2026-10-14T15:04:05Z");
    private final PublicarEventoDePruebaServicio servicio =
            new PublicarEventoDePruebaServicio(
                    repositorio, publicador, Clock.fixed(ahora, ZoneOffset.UTC));

    @Test
    void dadoElEventoDePrueba_cuandoSePublica_entoncesGuardaLaFilaYPublicaElMismoEvento() {
        UUID id = servicio.publicar();

        verify(repositorio).guardar(id, ahora);
        ArgumentCaptor<EventoSaliente> publicado = ArgumentCaptor.forClass(EventoSaliente.class);
        verify(publicador).publicar(publicado.capture());
        EventoSaliente evento = publicado.getValue();
        assertThat(evento.idEvento()).isEqualTo(id);
        assertThat(evento.tipo()).isEqualTo("generacion.EventoDePrueba");
        assertThat(evento.destino()).isEqualTo("usuarios");
        assertThat(evento.momento()).isEqualTo(ahora);
        assertThat(evento.contenido())
                .containsExactlyEntriesOf(java.util.Map.of("nota", "evento de prueba B00-H05"));
    }

    @Test
    void dadoDosPublicaciones_cuandoSePublican_entoncesTienenIdentificadoresDistintos() {
        assertThat(servicio.publicar()).isNotEqualTo(servicio.publicar());
    }
}
