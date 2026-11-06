package ec.mdd.generacion.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo.Accion;
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
 * El registro local no cambia, y cada cambio se publica como {@code generacion.EntradaBitacora}
 * hacia Usuarios en la misma transacción (F17-H01, research §1 y §2).
 */
class RegistroDeCambiosJdbcTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-06T15:00:00Z");
    private static final Instant PUBLICADO = Instant.parse("2026-11-06T15:00:01Z");
    private static final String QUIEN = "coordinacion.demo";

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

    private static Map<String, Object> detalle() {
        return Map.of(
                "periodo", "2026-I",
                "carrera", "TDS",
                "docente", "0100000001",
                "actividad", "TUTORIAS",
                "horas", "6",
                "horasAnteriores", "4");
    }

    private EventoEntreProcesos publicado() {
        assertThat(publicados).hasSize(1);
        return (EventoEntreProcesos) publicados.getFirst();
    }

    @Test
    void unaActividadDe4A6Horas_cuandoSeRegistra_entoncesPublicaSuEntradaDeBitacora() {
        registro.registrar(
                new CambioDeDistributivo(
                        MOMENTO,
                        QUIEN,
                        Accion.ACTIVIDAD_GUARDADA,
                        detalle(),
                        Map.of("horas", "4"),
                        Map.of("horas", "6")));

        EventoEntreProcesos evento = publicado();
        assertThat(evento.tipo()).isEqualTo("generacion.EntradaBitacora");
        assertThat(evento.origen()).isEqualTo("generacion");
        assertThat(evento.destino()).isEqualTo("usuarios");
        assertThat(evento.momento()).isEqualTo(PUBLICADO);
        assertThat(evento.contenido())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "usuario",
                                QUIEN,
                                "accion",
                                "ACTIVIDAD_GUARDADA",
                                "momento",
                                "2026-11-06T15:00:00Z",
                                "periodo",
                                "2026-I",
                                "objeto",
                                detalle(),
                                "anterior",
                                Map.of("horas", "4"),
                                "nuevo",
                                Map.of("horas", "6")));
    }

    @Test
    void unCambio_cuandoSeRegistra_entoncesGuardaLaFilaLocalComoAntesYDespuesPublica() {
        registro.registrar(
                new CambioDeDistributivo(
                        MOMENTO,
                        QUIEN,
                        Accion.ACTIVIDAD_GUARDADA,
                        Map.of("periodo", "2026-I"),
                        Map.of(),
                        Map.of("horas", "6")));

        InOrder orden = inOrder(sentencia);
        orden.verify(sentencia).param("usuario", QUIEN);
        orden.verify(sentencia).param("accion", "ACTIVIDAD_GUARDADA");
        orden.verify(sentencia).param("detalle", "{\"periodo\":\"2026-I\"}");
        orden.verify(sentencia).update();
        assertThat(publicados).hasSize(1);
    }

    @Test
    void unaAccionQueCrea_cuandoSeRegistra_entoncesLaEntradaNoLlevaAnterior() {
        registro.registrar(
                new CambioDeDistributivo(
                        MOMENTO,
                        QUIEN,
                        Accion.DISTRIBUTIVO_CREADO,
                        Map.of("periodo", "2026-I", "version", 1),
                        Map.of(),
                        Map.of("version", 1)));

        assertThat(publicado().contenido())
                .doesNotContainKey("anterior")
                .containsEntry("nuevo", Map.of("version", 1));
    }

    @Test
    void unaAccionQueQuita_cuandoSeRegistra_entoncesLaEntradaNoLlevaNuevo() {
        registro.registrar(
                new CambioDeDistributivo(
                        MOMENTO,
                        QUIEN,
                        Accion.PROPUESTA_QUITADA,
                        Map.of("periodo", "2026-I", "asignatura", "POO-204", "paralelo", "A"),
                        Map.of("docente", "0100000001", "justificacion", "AFINIDAD"),
                        Map.of()));

        assertThat(publicado().contenido())
                .doesNotContainKey("nuevo")
                .containsEntry(
                        "anterior", Map.of("docente", "0100000001", "justificacion", "AFINIDAD"));
    }

    @Test
    void unDetalleSinPeriodo_cuandoSeRegistra_entoncesLaEntradaNoLlevaPeriodo() {
        registro.registrar(
                new CambioDeDistributivo(
                        MOMENTO,
                        QUIEN,
                        Accion.ACTIVIDAD_GUARDADA,
                        Map.of("actividad", "140-b"),
                        Map.of(),
                        Map.of("horas", "6")));

        assertThat(publicado().contenido()).doesNotContainKey("periodo");
    }
}
