package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

/**
 * Escenario «Receptor caído» (FR-004 a FR-006, FR-011, SC-002, SC-003): con el receptor detenido la
 * transacción se confirma y la publicación queda pendiente; la tarea programada la reenvía cuando
 * vuelve. Con ciclos de 2 s y publicaciones atascadas a los 3 s, para que la prueba sea corta.
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
@Import(BaseDeEntrega.Contenedores.class)
@ExtendWith(OutputCaptureExtension.class)
class ReenvioIntegrationTest extends BaseDeEntrega {

    private static final Duration DOS_CICLOS_Y_MARGEN = Duration.ofSeconds(10);
    private static final List<String> PENDIENTES = List.of("FAILED", "RESUBMITTED", "PROCESSING");

    @AfterEach
    void levantarElReceptor() {
        try {
            RECEPTOR.levantar();
        } catch (RuntimeException yaLevantado) {
            // La prueba ya lo había vuelto a levantar.
        }
    }

    @Test
    void dadoElReceptorDetenido_cuandoSePublica_entoncesQuedaPendienteYSeReenviaAlVolver(
            CapturedOutput salida) {
        RECEPTOR.detener();

        EventoEntreProcesos evento = servicio.publicar(false);

        assertThat(filas(evento.idEvento())).isEqualTo(1);
        esperarPendiente(evento.idEvento());
        await().atMost(Duration.ofSeconds(5))
                .until(
                        () ->
                                salida.getAll()
                                        .contains(
                                                "Entrega fallida del evento " + evento.idEvento()));
        assertThat(recibidos(evento.idEvento())).isZero();

        RECEPTOR.levantar();

        esperarEstado(evento.idEvento(), "COMPLETED", DOS_CICLOS_Y_MARGEN);
        assertThat(recibidos(evento.idEvento())).isGreaterThanOrEqualTo(1);
        assertThat(salida.getAll())
                .contains("Reenvío del evento " + evento.idEvento())
                .doesNotContain("contenido-que-no-se-registra")
                .doesNotContain("clave-de-prueba");
    }

    @Test
    void dadoVariasPublicacionesConElReceptorDetenido_cuandoVuelve_entoncesSeCompletanTodas() {
        RECEPTOR.detener();

        List<EventoEntreProcesos> eventos =
                List.of(
                        servicio.publicar(false),
                        servicio.publicar(false),
                        servicio.publicar(false));
        eventos.forEach(evento -> esperarPendiente(evento.idEvento()));

        RECEPTOR.levantar();

        eventos.forEach(
                evento -> esperarEstado(evento.idEvento(), "COMPLETED", DOS_CICLOS_Y_MARGEN));
    }

    @Test
    void dadoPublicacionesAtascadasPorUnaCaida_cuandoPasaElPlazo_entoncesSeReenvianYSeCompletan() {
        // Una publicación real da el formato del registro; las atascadas son copias con otro
        // idEvento, como si el proceso se hubiera caído antes del oyente o a mitad de la entrega.
        EventoEntreProcesos modelo = servicio.publicar(false);
        esperarEstado(modelo.idEvento(), "COMPLETED", DOS_CICLOS_Y_MARGEN);
        Map<String, Object> fila =
                jdbc.sql(
                                "select listener_id, event_type, serialized_event"
                                        + " from eventos_prueba.event_publication"
                                        + " where serialized_event like ?")
                        .param("%" + modelo.idEvento() + "%")
                        .query()
                        .singleRow();

        UUID publicada = atascar(fila, modelo.idEvento(), "PUBLISHED");
        UUID enProceso = atascar(fila, modelo.idEvento(), "PROCESSING");

        esperarEstado(publicada, "COMPLETED", Duration.ofSeconds(15));
        esperarEstado(enProceso, "COMPLETED", Duration.ofSeconds(15));
        assertThat(recibidos(publicada)).isGreaterThanOrEqualTo(1);
        assertThat(recibidos(enProceso)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void dadoUnTokenRechazadoPorElReceptor_cuandoSeEntrega_entoncesPideOtroYSeCompleta() {
        int tokensAntes = RECEPTOR.tokensEmitidos();
        RECEPTOR.responderEventos(401);

        EventoEntreProcesos evento = servicio.publicar(false);

        esperarEstado(evento.idEvento(), "COMPLETED", Duration.ofSeconds(5));
        assertThat(recibidos(evento.idEvento())).isEqualTo(2);
        assertThat(RECEPTOR.tokensEmitidos()).isGreaterThan(tokensAntes);
    }

    @Test
    void dadoQueUsuariosNoEmiteElToken_cuandoSeEntrega_entoncesQuedaPendienteYSeCompletaDespues(
            CapturedOutput salida) {
        // El 401 descarta el token guardado; el 503 deja a Usuarios sin poder emitir otro.
        RECEPTOR.responderEventos(401);
        RECEPTOR.responderToken(503);

        EventoEntreProcesos evento = servicio.publicar(false);

        await().atMost(Duration.ofSeconds(5))
                .until(
                        () ->
                                salida.getAll()
                                        .contains(
                                                "Usuarios respondió 503 al pedir el token de proceso"));
        esperarEstado(evento.idEvento(), "COMPLETED", DOS_CICLOS_Y_MARGEN);
    }

    private void esperarPendiente(UUID idEvento) {
        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> PENDIENTES.contains(estado(idEvento)));
    }

    // Inserta una copia vieja de la publicación modelo con otro idEvento y el estado dado.
    private UUID atascar(Map<String, Object> modelo, UUID idModelo, String estadoAtascado) {
        UUID idEvento = UUID.randomUUID();
        jdbc.sql(
                        "insert into eventos_prueba.event_publication (id, listener_id, event_type,"
                                + " serialized_event, publication_date, status, completion_attempts)"
                                + " values (?, ?, ?, ?, now() - interval '10 minutes', ?, 0)")
                .params(
                        UUID.randomUUID(),
                        modelo.get("listener_id"),
                        modelo.get("event_type"),
                        modelo.get("serialized_event")
                                .toString()
                                .replace(idModelo.toString(), idEvento.toString()),
                        estadoAtascado)
                .update();
        return idEvento;
    }
}
