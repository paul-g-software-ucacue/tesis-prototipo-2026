package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.adaptadores.entrada.api.EventoPublicado;
import ec.mdd.generacion.adaptadores.entrada.api.EventosPruebaApi;
import ec.mdd.generacion.puertos.entrada.PublicarEventoDePrueba;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Evento de prueba de generacion ({@code contratos/generacion/openapi.yaml}, D-48).
 *
 * <p>Solo existe con {@code mdd.generacion.eventos-prueba.habilitado=true}, en local y en la CI;
 * apagado, la ruta no existe y responde 404. Responde 202: la entrega al otro proceso ocurre
 * después, a cargo del registro de publicaciones.
 */
@RestController
@ConditionalOnProperty(name = "mdd.generacion.eventos-prueba.habilitado", havingValue = "true")
class EventosPruebaControlador implements EventosPruebaApi {

    private final PublicarEventoDePrueba publicar;

    EventosPruebaControlador(PublicarEventoDePrueba publicar) {
        this.publicar = publicar;
    }

    @Override
    public ResponseEntity<EventoPublicado> publicarEventoDePrueba() {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new EventoPublicado(publicar.publicar()));
    }
}
