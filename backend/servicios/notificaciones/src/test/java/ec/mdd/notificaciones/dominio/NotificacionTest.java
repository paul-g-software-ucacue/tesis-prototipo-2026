package ec.mdd.notificaciones.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Una notificación: su contenido no cambia; solo se marca como leída, una vez (FR-008, FR-009). */
class NotificacionTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-15T15:04:05Z");

    @Test
    void dadaUnaSinLeer_cuandoSeMarca_entoncesQuedaLeidaConEseMomentoYElMismoContenido() {
        Notificacion sinLeer = notificacion(Optional.empty());
        Instant ahora = MOMENTO.plusSeconds(60);

        Notificacion leida = sinLeer.marcarLeida(ahora);

        assertThat(sinLeer.leida()).isFalse();
        assertThat(leida.leida()).isTrue();
        assertThat(leida.leidaEn()).contains(ahora);
        assertThat(leida).usingRecursiveComparison().ignoringFields("leidaEn").isEqualTo(sinLeer);
    }

    @Test
    void dadaUnaYaLeida_cuandoSeMarcaOtraVez_entoncesNoCambia() {
        Notificacion leida = notificacion(Optional.of(MOMENTO.plusSeconds(60)));

        assertThat(leida.marcarLeida(MOMENTO.plusSeconds(600))).isSameAs(leida);
    }

    @Test
    void dadosDatosQueFaltan_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                new Notificacion(
                                        null,
                                        UUID.randomUUID(),
                                        "vicerrectorado.demo",
                                        Optional.empty(),
                                        "2026-I",
                                        "Título",
                                        "",
                                        "/consolidado",
                                        "consolidacion.demo",
                                        "Responsable",
                                        MOMENTO,
                                        MOMENTO,
                                        Optional.empty()))
                .isInstanceOf(NullPointerException.class);
    }

    static Notificacion notificacion(Optional<Instant> leidaEn) {
        return new Notificacion(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "vicerrectorado.demo",
                Optional.empty(),
                "2026-I",
                "El distributivo llegó a «Revisión del Vicerrectorado»",
                "Periodo 2026-I, versión 1.",
                "/consolidado",
                "consolidacion.demo",
                "Responsable de consolidación",
                MOMENTO,
                MOMENTO.plusSeconds(1),
                leidaEn);
    }
}
