package ec.mdd.plataforma.eventos;

import java.time.Clock;
import java.util.Map;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplicación mínima con el registro de publicaciones en {@code eventos_prueba} y un servicio que
 * publica en su transacción, como Generación con el evento de prueba (D-48).
 */
@SpringBootApplication
class AplicacionDePruebaEventos {

    /** Inserta una fila en {@code eventos_prueba.fila_prueba} y publica, en una transacción. */
    @Service
    static class ServicioDePrueba {

        private final JdbcClient jdbc;
        private final ApplicationEventPublisher eventos;

        ServicioDePrueba(JdbcClient jdbc, ApplicationEventPublisher eventos) {
            this.jdbc = jdbc;
            this.eventos = eventos;
        }

        @Transactional
        EventoEntreProcesos publicar(boolean fallarDespues) {
            EventoEntreProcesos evento =
                    EventoEntreProcesos.nuevo(
                            "prueba.EventoDePrueba",
                            "prueba",
                            "usuarios",
                            Map.of("nota", "contenido-que-no-se-registra"),
                            Clock.systemUTC());
            jdbc.sql("insert into eventos_prueba.fila_prueba (id_evento) values (?)")
                    .param(evento.idEvento())
                    .update();
            eventos.publishEvent(evento);
            if (fallarDespues) {
                throw new IllegalStateException("fallo después de publicar");
            }
            return evento;
        }
    }
}
