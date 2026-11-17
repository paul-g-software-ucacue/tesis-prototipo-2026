package ec.mdd.notificaciones.adaptadores.entrada;

import ec.mdd.notificaciones.adaptadores.entrada.api.EventoEntreProcesos;
import ec.mdd.notificaciones.adaptadores.entrada.api.EventosApi;
import ec.mdd.notificaciones.puertos.entrada.EventoRecibido;
import ec.mdd.notificaciones.puertos.entrada.RecibirNotificaciones;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receptor de las notificaciones que piden otros procesos ({@code contratos/notificaciones},
 * F16-H01, ADR-018).
 *
 * <p>Solo lo alcanza un token de proceso ({@code /interno/**}); el API Gateway no lo enruta.
 * Responde 204 tanto a un evento nuevo como a uno ya registrado, para que el emisor cierre su
 * publicación aunque se haya perdido la primera respuesta.
 */
@RestController
class EventosNotificacionesControlador implements EventosApi {

    private final RecibirNotificaciones recibir;

    EventosNotificacionesControlador(RecibirNotificaciones recibir) {
        this.recibir = recibir;
    }

    @Override
    public ResponseEntity<Void> recibirEvento(EventoEntreProcesos evento) {
        recibir.recibir(
                new EventoRecibido(
                        evento.getIdEvento(),
                        evento.getTipo(),
                        evento.getOrigen(),
                        evento.getMomento().toInstant(),
                        evento.getContenido()));
        return ResponseEntity.noContent().build();
    }
}
