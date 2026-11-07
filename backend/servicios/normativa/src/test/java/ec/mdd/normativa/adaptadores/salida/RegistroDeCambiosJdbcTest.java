package ec.mdd.normativa.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.normativa.puertos.salida.CambioDeNormativa;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa.Accion;
import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * El registro local no cambia, y cada cambio se publica como {@code normativa.EntradaBitacora}
 * hacia Usuarios en la misma transacción (F17-H01, research §1 y §2).
 */
class RegistroDeCambiosJdbcTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-06T15:00:00Z");
    private static final Instant PUBLICADO = Instant.parse("2026-11-06T15:00:01Z");
    private static final String QUIEN = "vicerrectorado.demo";

    private final JdbcClient jdbc = mock(JdbcClient.class);
    private final JdbcClient.StatementSpec sentencia =
            mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
    private final List<Object> publicados = new ArrayList<>();
    private final RegistroDeCambiosJdbc registro =
            new RegistroDeCambiosJdbc(
                    jdbc, publicados::add, Clock.fixed(PUBLICADO, ZoneOffset.UTC));

    {
        when(jdbc.sql(anyString())).thenReturn(sentencia);
    }

    private EventoEntreProcesos publicado() {
        assertThat(publicados).hasSize(1);
        return (EventoEntreProcesos) publicados.getFirst();
    }

    @Test
    void unaVersionAsociada_cuandoSeRegistra_entoncesPublicaSuEntradaConElPeriodo() {
        registro.registrar(
                new CambioDeNormativa(
                        MOMENTO,
                        QUIEN,
                        Accion.VERSION_ASOCIADA,
                        Map.of("numero", 2, "periodo", "2026-I"),
                        Map.of(),
                        Map.of("version", 2)));

        EventoEntreProcesos evento = publicado();
        assertThat(evento.tipo()).isEqualTo("normativa.EntradaBitacora");
        assertThat(evento.origen()).isEqualTo("normativa");
        assertThat(evento.destino()).isEqualTo("usuarios");
        assertThat(evento.momento()).isEqualTo(PUBLICADO);
        assertThat(evento.contenido())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "usuario",
                                QUIEN,
                                "accion",
                                "VERSION_ASOCIADA",
                                "momento",
                                "2026-11-06T15:00:00Z",
                                "periodo",
                                "2026-I",
                                "objeto",
                                Map.of("numero", 2, "periodo", "2026-I"),
                                "nuevo",
                                Map.of("version", 2)));
    }

    @Test
    void unaEdicion_cuandoSeRegistra_entoncesGuardaLaFilaLocalYNoPerteneceAUnPeriodo() {
        registro.registrar(
                new CambioDeNormativa(
                        MOMENTO,
                        QUIEN,
                        Accion.VERSION_EDITADA,
                        Map.of("numero", 2),
                        Map.of("codigo", "POL-2024"),
                        Map.of("codigo", "POL-2025")));

        InOrder orden = inOrder(sentencia);
        orden.verify(sentencia).param("accion", "VERSION_EDITADA");
        orden.verify(sentencia).param("detalle", "{\"numero\":2}");
        orden.verify(sentencia).update();
        assertThat(publicado().contenido())
                .doesNotContainKey("periodo")
                .containsEntry("anterior", Map.of("codigo", "POL-2024"))
                .containsEntry("nuevo", Map.of("codigo", "POL-2025"));
    }
}
