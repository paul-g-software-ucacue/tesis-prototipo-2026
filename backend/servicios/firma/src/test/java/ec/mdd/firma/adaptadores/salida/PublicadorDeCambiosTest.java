package ec.mdd.firma.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.firma.puertos.salida.RegistroDeCambios.CambioDeFirma;
import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Cada cambio de Firma va a la bitácora como un evento hacia Usuarios (F13-H02, ADR-018). */
class PublicadorDeCambiosTest {

    private static final Instant PUBLICADO = Instant.parse("2026-11-11T15:00:06Z");
    private static final Instant MOMENTO = Instant.parse("2026-11-11T15:00:05Z");

    private final List<Object> publicados = new ArrayList<>();
    private final PublicadorDeCambios publicador =
            new PublicadorDeCambios(publicados::add, Clock.fixed(PUBLICADO, ZoneOffset.UTC));

    @Test
    void dadoElDocumentoGenerado_cuandoSeRegistra_entoncesUnEventoDeFirmaHaciaUsuarios() {
        publicador.registrar(
                new CambioDeFirma(
                        MOMENTO,
                        "sistema",
                        CambioDeFirma.DOCUMENTO_FIRMABLE_GENERADO,
                        Map.of("periodo", "2026-I", "version", 1, "sha256", "b".repeat(64)),
                        Map.of("estado", "GENERADO")));

        assertThat(publicados).hasSize(1);
        EventoEntreProcesos evento = (EventoEntreProcesos) publicados.getFirst();
        assertThat(evento.tipo()).isEqualTo("firma.EntradaBitacora");
        assertThat(evento.origen()).isEqualTo("firma");
        assertThat(evento.destino()).isEqualTo("usuarios");
        assertThat(evento.contenido())
                .containsEntry("usuario", "sistema")
                .containsEntry("accion", "DOCUMENTO_FIRMABLE_GENERADO")
                .containsEntry("momento", "2026-11-11T15:00:05Z")
                .containsEntry("periodo", "2026-I")
                .containsEntry("nuevo", Map.of("estado", "GENERADO"))
                .doesNotContainKey("anterior");
    }
}
