package ec.mdd.roles.adaptadores.entrada;

import ec.mdd.roles.adaptadores.entrada.api.EventoEntreProcesos;
import ec.mdd.roles.adaptadores.entrada.api.EventosApi;
import ec.mdd.roles.puertos.entrada.EventoRecibido;
import ec.mdd.roles.puertos.entrada.RecibirEventos;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receptor de eventos de otros procesos ({@code contratos/roles/openapi.yaml}, F05-H03, ADR-018).
 *
 * <p>Solo lo alcanza un token de proceso ({@code /interno/**}); el API Gateway no lo enruta.
 * Responde 204 tanto a un evento nuevo como a uno ya aplicado, para que el emisor cierre su
 * publicación aunque se haya perdido la primera respuesta.
 */
@RestController
class EventosRolesControlador implements EventosApi {

    private final RecibirEventos recibir;

    EventosRolesControlador(RecibirEventos recibir) {
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
