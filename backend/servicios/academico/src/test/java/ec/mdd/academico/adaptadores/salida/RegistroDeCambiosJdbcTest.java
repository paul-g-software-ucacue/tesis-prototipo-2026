package ec.mdd.academico.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
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
 * El registro local no cambia, y cada cambio se publica como {@code academico.EntradaBitacora}
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

    private Map<String, Object> registrar(
            Accion accion, Map<String, Object> detalle, Map<String, Object> anterior) {
        registro.registrar(
                new CambioRegistrado(
                        MOMENTO, QUIEN, accion, detalle, anterior, Map.of("estado", "NUEVO")));
        return publicado().contenido();
    }

    @Test
    void unCambioDeEstadoDeUnPeriodo_cuandoSeRegistra_entoncesPublicaSuEntradaConElPeriodo() {
        Map<String, Object> detalle =
                Map.of("codigo", "2026-I", "anterior", "PLANIFICACION", "nuevo", "EN_CURSO");

        registro.registrar(
                new CambioRegistrado(
                        MOMENTO,
                        QUIEN,
                        Accion.PERIODO_ESTADO_CAMBIADO,
                        detalle,
                        Map.of("estado", "PLANIFICACION"),
                        Map.of("estado", "EN_CURSO")));

        EventoEntreProcesos evento = publicado();
        assertThat(evento.tipo()).isEqualTo("academico.EntradaBitacora");
        assertThat(evento.origen()).isEqualTo("academico");
        assertThat(evento.destino()).isEqualTo("usuarios");
        assertThat(evento.momento()).isEqualTo(PUBLICADO);
        assertThat(evento.contenido())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "usuario",
                                QUIEN,
                                "accion",
                                "PERIODO_ESTADO_CAMBIADO",
                                "momento",
                                "2026-11-06T15:00:00Z",
                                "periodo",
                                "2026-I",
                                "objeto",
                                detalle,
                                "anterior",
                                Map.of("estado", "PLANIFICACION"),
                                "nuevo",
                                Map.of("estado", "EN_CURSO")));
    }

    @Test
    void unCambio_cuandoSeRegistra_entoncesGuardaLaFilaLocalComoAntesYDespuesPublica() {
        registrar(Accion.UNIDAD_INACTIVADA, Map.of("codigo", "COM-EVAL"), Map.of("activa", true));

        InOrder orden = inOrder(sentencia);
        orden.verify(sentencia).param("accion", "UNIDAD_INACTIVADA");
        orden.verify(sentencia).param("detalle", "{\"codigo\":\"COM-EVAL\"}");
        orden.verify(sentencia).update();
    }

    @Test
    void unaOfertaOUnaMatricula_cuandoSeRegistra_entoncesSuPeriodoEsElDelDetalle() {
        assertThat(
                        registrar(
                                Accion.ASIGNATURA_PARALELO_RETIRADA,
                                Map.of("periodo", "2026-I", "asignatura", "A1", "paralelo", "A"),
                                Map.of("estado", "OFERTADA")))
                .containsEntry("periodo", "2026-I");
        publicados.clear();
        assertThat(
                        registrar(
                                Accion.MATRICULA_CORREGIDA,
                                Map.of("periodo", "2026-II", "carrera", "C1"),
                                Map.of("total", 1)))
                .containsEntry("periodo", "2026-II");
    }

    @Test
    void unCambioDeLaEstructuraOUnaCarrera_cuandoSeRegistra_entoncesNoPerteneceAUnPeriodo() {
        assertThat(
                        registrar(
                                Accion.CARRERA_ESTADO_CAMBIADO,
                                Map.of("codigo", "C1", "periodo", "no-es-un-periodo"),
                                Map.of("estado", "VIGENTE")))
                .doesNotContainKey("periodo");
    }

    @Test
    void unaAccionQueCrea_cuandoSeRegistra_entoncesLaEntradaNoLlevaAnterior() {
        registro.registrar(
                new CambioRegistrado(
                        MOMENTO,
                        QUIEN,
                        Accion.MALLA_REGISTRADA,
                        Map.of("codigo", "DS-2019", "carrera", "C1"),
                        Map.of(),
                        Map.of("codigo", "DS-2019", "carrera", "C1")));

        assertThat(publicado().contenido()).doesNotContainKey("anterior").containsKey("nuevo");
    }
}
