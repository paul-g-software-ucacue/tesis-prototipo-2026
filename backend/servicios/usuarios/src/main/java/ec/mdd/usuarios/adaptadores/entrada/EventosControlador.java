package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.usuarios.adaptadores.entrada.api.EventoEntreProcesos;
import ec.mdd.usuarios.adaptadores.entrada.api.EventosApi;
import ec.mdd.usuarios.puertos.entrada.EventoRecibido;
import ec.mdd.usuarios.puertos.entrada.RegistrarEventoRecibido;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receptor de eventos de otros procesos ({@code contratos/usuarios/openapi.yaml}, D-47, ADR-018).
 *
 * <p>Solo lo alcanza un token de proceso ({@code /interno/**}); el API Gateway no lo enruta.
 * Responde 204 tanto a un evento nuevo como a uno ya registrado, para que el emisor cierre su
 * publicación aunque se haya perdido la primera respuesta.
 */
@RestController
class EventosControlador implements EventosApi {

    private final RegistrarEventoRecibido registrar;

    EventosControlador(RegistrarEventoRecibido registrar) {
        this.registrar = registrar;
    }

    @Override
    public ResponseEntity<Void> recibirEvento(EventoEntreProcesos evento) {
        registrar.registrar(
                new EventoRecibido(
                        evento.getIdEvento(),
                        evento.getTipo(),
                        evento.getOrigen(),
                        evento.getMomento().toInstant(),
                        evento.getContenido()));
        return ResponseEntity.noContent().build();
    }
}
