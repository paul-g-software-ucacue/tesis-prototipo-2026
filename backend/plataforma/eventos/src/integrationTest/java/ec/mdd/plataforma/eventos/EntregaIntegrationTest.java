package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.AsyncConfigurer;

/**
 * Escenario «Entrega normal» y reversión (FR-001 a FR-004, SC-001, SC-005): un evento publicado en
 * una transacción llega una vez al receptor y su publicación queda completada; una transacción
 * revertida no deja nada.
 */
@SpringBootTest(classes = AplicacionDePruebaEventos.class)
@Import(BaseDeEntrega.Contenedores.class)
@ExtendWith(OutputCaptureExtension.class)
class EntregaIntegrationTest extends BaseDeEntrega {

    @Test
    void dadoElReceptorDisponible_cuandoSePublica_entoncesLoRecibeUnaVezYSeCompleta(
            CapturedOutput salida) {
        EventoEntreProcesos evento = servicio.publicar(false);

        esperarEstado(evento.idEvento(), "COMPLETED", Duration.ofSeconds(10));
        assertThat(recibidos(evento.idEvento())).isEqualTo(1);
        assertThat(filas(evento.idEvento())).isEqualTo(1);
        assertThat(RECEPTOR.eventosRecibidos().getLast().autorizacion())
                .startsWith("Bearer token-");
        assertThat(salida.getAll()).doesNotContain("contenido-que-no-se-registra");
    }

    @Test
    void dadoElContexto_cuandoArranca_entoncesLasEntregasFallidasUsanElManejadorDeEventos(
            @Autowired AsyncConfigurer configuracion) {
        assertThat(configuracion.getAsyncUncaughtExceptionHandler())
                .isInstanceOf(FallosAsincronos.class);
        assertThat(configuracion.getAsyncExecutor()).isNotNull();
    }

    @Test
    void dadoUnaTransaccionQueSeRevierte_cuandoPublico_entoncesNoQuedaNiLaFilaNiLaPublicacion()
            throws InterruptedException {
        int antes = RECEPTOR.eventosRecibidos().size();

        assertThatThrownBy(() -> servicio.publicar(true)).isInstanceOf(IllegalStateException.class);

        Thread.sleep(1_000);
        assertThat(
                        jdbc.sql(
                                        "select count(*) from eventos_prueba.event_publication"
                                                + " where status <> 'COMPLETED'")
                                .query(Long.class)
                                .single())
                .isZero();
        assertThat(RECEPTOR.eventosRecibidos()).hasSize(antes);
        assertThat(
                        jdbc.sql("select count(*) from eventos_prueba.fila_prueba")
                                .query(Long.class)
                                .single())
                .isEqualTo(
                        jdbc.sql("select count(*) from eventos_prueba.event_publication")
                                .query(Long.class)
                                .single());
    }
}
