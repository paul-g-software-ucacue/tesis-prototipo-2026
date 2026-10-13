package ec.mdd.plataforma.errores;

import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.RespuestaError;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La página {@code /error} propia: los errores que no pasan por {@link ManejadorDeErrores}, como
 * los de un filtro, también responden con el esquema {@code Error} (FR-010, ADR-016).
 *
 * <p>Un 5xx (salvo 503) registra el incidente y responde 500 con su {@code idIncidente}. El 403 de
 * la seguridad se responde sin cuerpo: su formato llega con F05-H02 (matriz de permisos).
 */
@RestController
public class ControladorDeErrores implements ErrorController {

    /** Escribe el error de la petición original. */
    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<RespuestaError> error(HttpServletRequest peticion) {
        int estado =
                peticion.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) instanceof Integer codigo
                        ? codigo
                        : 500;
        if (estado == 403) {
            return ResponseEntity.status(403).build();
        }
        if (estado >= 500 && estado != 503) {
            String id =
                    RegistroDeIncidentes.registrar(
                            (Throwable) peticion.getAttribute(RequestDispatcher.ERROR_EXCEPTION),
                            peticion.getMethod(),
                            (String) peticion.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(RespuestaError.deIncidente(CodigosComunes.ERROR_INTERNO, id));
        }
        return ResponseEntity.status(estado)
                .contentType(MediaType.APPLICATION_JSON)
                .body(RespuestaError.deCatalogo(CodigosComunes.segunEstado(estado)));
    }
}
