package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * La entrega local dentro de un proceso (F17-H01, ADR-024, FR-006 y FR-007): si el receptor local
 * falla, la publicación queda pendiente como una HTTP, y el reenvío la completa cuando acepta, sin
 * entregarla dos veces. No pasa por HTTP aunque haya un destino configurado.
 */
@SpringBootTest(
        classes = AplicacionDePruebaEventos.class,
        properties = {
            "mdd.eventos.reenvio.intervalo=PT2S",
            "spring.modulith.events.staleness.check-interval=PT1S",
            "spring.modulith.events.staleness.published=PT3S",
            "spring.modulith.events.staleness.processing=PT3S",
            "spring.modulith.events.staleness.resubmitted=PT3S"
        })
@Import({BaseDeEntrega.Contenedores.class, ReceptorLocalIntegrationTest.ConReceptorLocal.class})
class ReceptorLocalIntegrationTest extends BaseDeEntrega {

    private static final Duration DOS_CICLOS_Y_MARGEN = Duration.ofSeconds(10);
    private static final List<String> PENDIENTES = List.of("FAILED", "RESUBMITTED", "PROCESSING");

    @Autowired private ReceptorQueFallaPrimero receptor;

    @AfterEach
    void aceptarOtraVez() {
        receptor.fallar.set(false);
    }

    @Test
    void unReceptorLocalQueFalla_cuandoVuelveAAceptar_entoncesElReenvioLaCompletaUnaVez() {
        receptor.fallar.set(true);

        EventoEntreProcesos evento = servicio.publicar(false);

        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> PENDIENTES.contains(estado(evento.idEvento())));
        assertThat(receptor.intentos.get()).isPositive();
        assertThat(receptor.aceptados).doesNotContain(evento.idEvento());

        receptor.fallar.set(false);

        esperarEstado(evento.idEvento(), "COMPLETED", DOS_CICLOS_Y_MARGEN);
        assertThat(receptor.aceptados).containsOnlyOnce(evento.idEvento());
        assertThat(recibidos(evento.idEvento())).as("ninguna entrega por HTTP").isZero();
    }

    @Test
    void unReceptorLocalQueAcepta_cuandoSePublica_entoncesSeCompletaSinHttp() {
        EventoEntreProcesos evento = servicio.publicar(false);

        esperarEstado(evento.idEvento(), "COMPLETED", Duration.ofSeconds(5));
        assertThat(receptor.aceptados).containsOnlyOnce(evento.idEvento());
        assertThat(recibidos(evento.idEvento())).isZero();
    }

    /** Un receptor local de {@code usuarios} que falla mientras se le pida. */
    static final class ReceptorQueFallaPrimero implements ReceptorLocal {

        final AtomicBoolean fallar = new AtomicBoolean();
        final AtomicInteger intentos = new AtomicInteger();
        final List<UUID> aceptados = new CopyOnWriteArrayList<>();

        @Override
        public String destino() {
            return "usuarios";
        }

        @Override
        public void recibir(EventoEntreProcesos evento) {
            intentos.incrementAndGet();
            if (fallar.get()) {
                throw new IllegalStateException("el receptor local no está listo");
            }
            aceptados.add(evento.idEvento());
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ConReceptorLocal {

        @Bean
        ReceptorQueFallaPrimero receptorQueFallaPrimero() {
            return new ReceptorQueFallaPrimero();
        }
    }
}
