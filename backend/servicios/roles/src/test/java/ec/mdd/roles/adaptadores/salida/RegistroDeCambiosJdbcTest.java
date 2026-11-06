package ec.mdd.roles.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import ec.mdd.roles.puertos.salida.CambioDeRoles;
import ec.mdd.roles.puertos.salida.CambioDeRoles.Accion;
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
 * El registro local no cambia, y cada cambio se publica como {@code roles.EntradaBitacora} hacia
 * Usuarios en la misma transacción; dentro de {@code identidad} se entrega sin HTTP (F17-H01,
 * ADR-024).
 */
class RegistroDeCambiosJdbcTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-06T15:00:00Z");
    private static final Instant PUBLICADO = Instant.parse("2026-11-06T15:00:01Z");
    private static final String QUIEN = "admin.demo";

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
    void unaDesignacion_cuandoSeRegistra_entoncesPublicaSuEntradaConElPeriodo() {
        Map<String, Object> detalle =
                Map.of("periodo", "2026-I", "anterior", "uno.demo", "nuevo", "otro.demo");

        registro.registrar(
                new CambioDeRoles(
                        MOMENTO,
                        QUIEN,
                        Accion.DESIGNAR_RESPONSABLE,
                        detalle,
                        Map.of("responsable", "uno.demo"),
                        Map.of("responsable", "otro.demo")));

        EventoEntreProcesos evento = publicado();
        assertThat(evento.tipo()).isEqualTo("roles.EntradaBitacora");
        assertThat(evento.origen()).isEqualTo("roles");
        assertThat(evento.destino()).isEqualTo("usuarios");
        assertThat(evento.momento()).isEqualTo(PUBLICADO);
        assertThat(evento.contenido())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "usuario",
                                QUIEN,
                                "accion",
                                "DESIGNAR_RESPONSABLE",
                                "momento",
                                "2026-11-06T15:00:00Z",
                                "periodo",
                                "2026-I",
                                "objeto",
                                detalle,
                                "anterior",
                                Map.of("responsable", "uno.demo"),
                                "nuevo",
                                Map.of("responsable", "otro.demo")));
    }

    @Test
    void unaAsignacion_cuandoSeRegistra_entoncesGuardaLaFilaYNoPerteneceAUnPeriodo() {
        registro.registrar(
                new CambioDeRoles(
                        MOMENTO,
                        QUIEN,
                        Accion.ASIGNAR_ROL,
                        Map.of("usuario", "docente.demo"),
                        Map.of("roles", List.of()),
                        Map.of("roles", List.of("DOCENTE"))));

        InOrder orden = inOrder(sentencia);
        orden.verify(sentencia).param("accion", "ASIGNAR_ROL");
        orden.verify(sentencia).param("detalle", "{\"usuario\":\"docente.demo\"}");
        orden.verify(sentencia).update();
        assertThat(publicado().contenido())
                .doesNotContainKey("periodo")
                .containsEntry("nuevo", Map.of("roles", List.of("DOCENTE")));
    }

    @Test
    void borrarUnRol_cuandoSeRegistra_entoncesLaEntradaNoLlevaNuevo() {
        registro.registrar(
                new CambioDeRoles(
                        MOMENTO,
                        QUIEN,
                        Accion.BORRAR_ROL,
                        Map.of("identificador", "REVISOR", "nombre", "Revisor"),
                        Map.of("nombre", "Revisor"),
                        Map.of()));

        assertThat(publicado().contenido()).doesNotContainKey("nuevo").containsKey("anterior");
    }
}
