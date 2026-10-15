package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.ResubmissionOptions;

/** La tarea de reenvío con un {@code FailedEventPublications} escrito a mano. */
@ExtendWith(OutputCaptureExtension.class)
class ReenvioDePublicacionesTest {

    private final List<ResubmissionOptions> llamadas = new ArrayList<>();
    private final ReenvioDePublicaciones reenvio = new ReenvioDePublicaciones(llamadas::add);

    @Test
    void dadoUnCiclo_cuandoSeEjecuta_entoncesReenviaLasFallidasConLasOpcionesPorDefecto() {
        reenvio.reenviar();
        reenvio.reenviar();

        assertThat(llamadas).hasSize(2);
        ResubmissionOptions porDefecto = ResubmissionOptions.defaults();
        assertThat(llamadas.getFirst().getBatchSize()).isEqualTo(porDefecto.getBatchSize());
        assertThat(llamadas.getFirst().getMaxInFlight()).isEqualTo(porDefecto.getMaxInFlight());
        assertThat(llamadas.getFirst().getMinAge()).isEqualTo(porDefecto.getMinAge());
    }

    @Test
    void dadoUnaPublicacionDeUnEvento_cuandoSeFiltra_entoncesLaRegistraSinElContenido(
            CapturedOutput salida) {
        EventoEntreProcesos evento =
                EventoEntreProcesos.nuevo(
                        "generacion.EventoDePrueba",
                        "generacion",
                        "usuarios",
                        Map.of("nota", "contenido-que-no-se-registra"),
                        Clock.systemUTC());
        reenvio.reenviar();

        boolean reenviar = llamadas.getFirst().getFilter().test(publicacion(evento));
        boolean otra = llamadas.getFirst().getFilter().test(publicacion("otro evento"));

        assertThat(reenviar).isTrue();
        assertThat(otra).isTrue();
        assertThat(salida.getAll())
                .contains(
                        "Reenvío del evento "
                                + evento.idEvento()
                                + " de tipo generacion.EventoDePrueba")
                .contains("Reenvío de la publicación")
                .doesNotContain("contenido-que-no-se-registra");
    }

    private static EventPublication publicacion(Object evento) {
        UUID id = UUID.randomUUID();
        return new EventPublication() {
            @Override
            public UUID getIdentifier() {
                return id;
            }

            @Override
            public Object getEvent() {
                return evento;
            }

            @Override
            public Instant getPublicationDate() {
                return Instant.now();
            }

            @Override
            public Optional<Instant> getCompletionDate() {
                return Optional.empty();
            }

            @Override
            public Status getStatus() {
                return Status.FAILED;
            }

            @Override
            public Instant getLastResubmissionDate() {
                return null;
            }

            @Override
            public int getCompletionAttempts() {
                return 1;
            }
        };
    }
}
