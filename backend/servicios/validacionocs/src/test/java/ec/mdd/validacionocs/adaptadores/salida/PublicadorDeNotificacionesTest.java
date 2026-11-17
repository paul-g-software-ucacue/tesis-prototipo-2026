package ec.mdd.validacionocs.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import ec.mdd.validacionocs.dominio.EstadoDeEnvio;
import ec.mdd.validacionocs.dominio.EtapaDelProceso;
import ec.mdd.validacionocs.dominio.Observacion;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Cada transición se publica como {@code validacionocs.NotificacionesSolicitadas} hacia
 * Notificaciones, con el contenido {@code SolicitudDeNotificaciones} (F16-H01,
 * contracts/eventos.md).
 */
class PublicadorDeNotificacionesTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-15T15:00:00Z");
    private static final Instant PUBLICADO = Instant.parse("2026-11-15T15:00:01Z");
    private static final String HASH = "a".repeat(64);

    private final List<Object> publicados = new ArrayList<>();
    private final AtomicInteger consultas = new AtomicInteger();
    private Map<String, String> nombres = Map.of("DS", "DESARROLLO DE SOFTWARE");
    private final PublicadorDeNotificaciones publicador =
            new PublicadorDeNotificaciones(
                    publicados::add,
                    () -> {
                        consultas.incrementAndGet();
                        if (nombres == null) {
                            throw new IllegalStateException("Académico no responde");
                        }
                        return nombres;
                    },
                    Clock.fixed(PUBLICADO, ZoneOffset.UTC));

    @Test
    void dadoUnPasoDeEtapa_cuandoSeNotifica_entoncesPublicaUnEventoConSuSolicitud() {
        Transicion paso =
                new Transicion(
                        TipoDeTransicion.PASO_DE_ETAPA,
                        TipoDeEtapa.EN_CONSOLIDACION,
                        TipoDeEtapa.EN_REVISION_VRA,
                        Optional.empty(),
                        1,
                        HASH,
                        "consolidacion.demo",
                        MOMENTO);

        publicador.notificar(proceso(), List.of(paso));

        // Un paso de etapa no nombra carreras: no se le pregunta a Académico.
        assertThat(consultas).hasValue(0);
        assertThat(publicados).hasSize(1);
        EventoEntreProcesos evento = (EventoEntreProcesos) publicados.getFirst();
        assertThat(evento.tipo()).isEqualTo("validacionocs.NotificacionesSolicitadas");
        assertThat(evento.origen()).isEqualTo("validacionocs");
        assertThat(evento.destino()).isEqualTo("notificaciones");
        assertThat(evento.momento()).isEqualTo(PUBLICADO);
        assertThat(evento.contenido())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "periodo", "2026-I",
                                "autor", "consolidacion.demo",
                                "momento", "2026-11-15T15:00:00Z",
                                "mensajes",
                                        List.of(
                                                Map.of(
                                                        "destino",
                                                        Map.of("rol", "VICERRECTOR"),
                                                        "titulo",
                                                        "El distributivo llegó a «Revisión"
                                                                + " del Vicerrectorado»",
                                                        "detalle",
                                                        "Periodo 2026-I, versión 1.",
                                                        "enlace",
                                                        "/consolidado"))));
    }

    @Test
    void dadaUnaDevolucion_cuandoSeNotifica_entoncesCadaDestinoLlevaSuCarrera() {
        Transicion devolucion =
                new Transicion(
                        TipoDeTransicion.DEVOLUCION,
                        TipoDeEtapa.EN_CONSOLIDACION,
                        TipoDeEtapa.BORRADOR_CARRERA,
                        Optional.empty(),
                        1,
                        HASH,
                        "consolidacion.demo",
                        MOMENTO,
                        Map.of("DS", new Observacion("Falta el docente de Redes II")));

        publicador.notificar(proceso(), List.of(devolucion));

        EventoEntreProcesos evento = (EventoEntreProcesos) publicados.getFirst();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mensajes =
                (List<Map<String, Object>>) evento.contenido().get("mensajes");
        assertThat(mensajes.getFirst().get("destino"))
                .isEqualTo(Map.of("rol", "COORDINADOR_CARRERA", "carrera", "DS"));
    }

    @Test
    void dadasDosTransiciones_cuandoSeNotifican_entoncesUnEventoPorCadaUna() {
        Transicion envio =
                new Transicion(
                        TipoDeTransicion.ENVIO_PARCIAL,
                        TipoDeEtapa.BORRADOR_CARRERA,
                        TipoDeEtapa.BORRADOR_CARRERA,
                        Optional.of("DS"),
                        1,
                        HASH,
                        "coordinacion.demo",
                        MOMENTO);
        Transicion paso =
                new Transicion(
                        TipoDeTransicion.PASO_DE_ETAPA,
                        TipoDeEtapa.BORRADOR_CARRERA,
                        TipoDeEtapa.EN_CONSOLIDACION,
                        Optional.empty(),
                        1,
                        HASH,
                        "coordinacion.demo",
                        MOMENTO);

        publicador.notificar(proceso(), List.of(envio, paso));

        assertThat(publicados).hasSize(2);
        assertThat(publicados)
                .extracting(evento -> ((EventoEntreProcesos) evento).idEvento())
                .doesNotHaveDuplicates();
    }

    @Test
    void dadoElEnvioDeUnParcial_cuandoSeNotifica_entoncesElTituloNombraLaCarrera() {
        publicador.notificar(proceso(), List.of(envio()));

        assertThat(titulos()).containsExactly("La carrera DESARROLLO DE SOFTWARE envió su parcial");
        assertThat(consultas).hasValue(1);
    }

    @Test
    void
            dadoQueAcademicoNoResponde_cuandoSeNotifica_entoncesElTituloLlevaElCodigoYSePublicaIgual() {
        nombres = null;

        publicador.notificar(proceso(), List.of(envio()));

        assertThat(titulos()).containsExactly("La carrera DS envió su parcial");
    }

    @Test
    void sinTransicionesNuevas_cuandoSeNotifica_entoncesNoPublicaNada() {
        publicador.notificar(proceso(), List.of());

        assertThat(publicados).isEmpty();
    }

    private static Transicion envio() {
        return new Transicion(
                TipoDeTransicion.ENVIO_PARCIAL,
                TipoDeEtapa.BORRADOR_CARRERA,
                TipoDeEtapa.BORRADOR_CARRERA,
                Optional.of("DS"),
                1,
                HASH,
                "coordinacion.demo",
                MOMENTO);
    }

    @SuppressWarnings("unchecked")
    private List<Object> titulos() {
        return publicados.stream()
                .flatMap(
                        evento ->
                                ((List<Map<String, Object>>)
                                                ((EventoEntreProcesos) evento)
                                                        .contenido()
                                                        .get("mensajes"))
                                        .stream())
                .map(mensaje -> mensaje.get("titulo"))
                .toList();
    }

    private static ProcesoAprobacion proceso() {
        return ProcesoAprobacion.de(
                "2026-I",
                Arrays.stream(TipoDeEtapa.values())
                        .map(
                                tipo ->
                                        new EtapaDelProceso(
                                                tipo,
                                                tipo != TipoDeEtapa.BORRADOR_UNIDADES
                                                        && tipo != TipoDeEtapa.REMITIDO_ZONAL,
                                                tipo == TipoDeEtapa.EN_REVISION_VRA
                                                        ? "Revisión del Vicerrectorado"
                                                        : tipo.name(),
                                                switch (tipo) {
                                                    case BORRADOR_CARRERA -> "COORDINADOR_CARRERA";
                                                    case EN_REVISION_VRA -> "VICERRECTOR";
                                                    default -> "RESPONSABLE_CONSOLIDACION";
                                                },
                                                false))
                        .toList(),
                TipoDeEtapa.EN_REVISION_VRA,
                MOMENTO,
                Map.of("DS", EstadoDeEnvio.ENVIADO),
                List.of());
    }
}
