package ec.mdd.validacionocs.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Cada carga se publica como {@code validacionocs.EntradaBitacora} hacia Usuarios (F17-H01). */
class PublicadorDeCambiosTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-06T15:00:00Z");
    private static final Instant PUBLICADO = Instant.parse("2026-11-06T15:00:01Z");

    private final List<Object> publicados = new ArrayList<>();
    private final PublicadorDeCambios publicador =
            new PublicadorDeCambios(publicados::add, Clock.fixed(PUBLICADO, ZoneOffset.UTC));

    private EventoEntreProcesos publicado() {
        assertThat(publicados).hasSize(1);
        return (EventoEntreProcesos) publicados.getFirst();
    }

    @Test
    void otraCargaDelPeriodo_cuandoSeRegistra_entoncesPublicaSuEntradaConElPeriodo() {
        publicador.registrar(
                new CambioDeFlujo(
                        MOMENTO,
                        "vicerrectorado.demo",
                        CambioDeFlujo.Accion.CONFIGURACION_FLUJO_CARGADA,
                        Map.of("periodo", "2026-I"),
                        Map.of("version", 1),
                        Map.of("version", 2)));

        EventoEntreProcesos evento = publicado();
        assertThat(evento.tipo()).isEqualTo("validacionocs.EntradaBitacora");
        assertThat(evento.origen()).isEqualTo("validacionocs");
        assertThat(evento.destino()).isEqualTo("usuarios");
        assertThat(evento.momento()).isEqualTo(PUBLICADO);
        assertThat(evento.contenido())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "usuario", "vicerrectorado.demo",
                                "accion", "CONFIGURACION_FLUJO_CARGADA",
                                "momento", "2026-11-06T15:00:00Z",
                                "periodo", "2026-I",
                                "objeto", Map.of("periodo", "2026-I"),
                                "anterior", Map.of("version", 1),
                                "nuevo", Map.of("version", 2)));
    }

    @Test
    void laPrimeraCarga_cuandoSeRegistra_entoncesLaEntradaNoLlevaAnterior() {
        publicador.registrar(
                new CambioDeFlujo(
                        MOMENTO,
                        "vicerrectorado.demo",
                        CambioDeFlujo.Accion.CONFIGURACION_FLUJO_CARGADA,
                        Map.of("periodo", "2026-I"),
                        Map.of(),
                        Map.of("version", 1)));

        assertThat(publicado().contenido()).doesNotContainKey("anterior").containsKey("nuevo");
    }
}
