package ec.mdd.gateway;

import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.Incidentes;
import ec.mdd.plataforma.errores.nucleo.RespuestaError;
import ec.mdd.plataforma.errores.nucleo.SaneadorDeTrazas;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * Los errores propios del API Gateway con el modelo común (D-40, ADR-016).
 *
 * <ul>
 *   <li>Ruta sin destino o método no permitido: 404 o 405 con su código común.
 *   <li>Sin conexión con el proceso de la ruta: 503 {@code COMUN_SERVICIO_NO_DISPONIBLE}, sin la
 *       dirección interna.
 *   <li>Cualquier otro error: 500 {@code COMUN_ERROR_INTERNO} con {@code idIncidente}, y la traza
 *       saneada en el registro.
 * </ul>
 *
 * <p>Va antes que el manejador de Spring Boot ({@code @Order(-2)}). El 401 por token lo sigue
 * escribiendo la seguridad (ADR-014), y las respuestas de error de los procesos pasan sin cambios.
 */
@Component
@Order(-2)
class ManejadorDeErroresGateway implements ErrorWebExceptionHandler {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ManejadorDeErroresGateway.class);

    // Por nombre, para no depender de Netty en el código.
    private static final String TIEMPO_DE_CONEXION = "io.netty.channel.ConnectTimeoutException";

    private final ObjectMapper json;

    ManejadorDeErroresGateway(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange intercambio, Throwable error) {
        ServerHttpResponse respuesta = intercambio.getResponse();
        if (respuesta.isCommitted()) {
            return Mono.error(error);
        }
        int estado;
        RespuestaError cuerpo;
        if (error instanceof ResponseStatusException conEstado
                && (conEstado.getStatusCode().value() < 500
                        || conEstado.getStatusCode().value() == 503)) {
            estado = conEstado.getStatusCode().value();
            cuerpo = RespuestaError.deCatalogo(CodigosComunes.segunEstado(estado));
        } else if (esFallaDeConexion(error)) {
            estado = 503;
            cuerpo = RespuestaError.deCatalogo(CodigosComunes.SERVICIO_NO_DISPONIBLE);
        } else {
            estado = 500;
            cuerpo =
                    RespuestaError.deIncidente(
                            CodigosComunes.ERROR_INTERNO, registrar(intercambio, error));
        }
        respuesta.setStatusCode(HttpStatusCode.valueOf(estado));
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return respuesta.writeWith(
                Mono.just(respuesta.bufferFactory().wrap(json.writeValueAsBytes(cuerpo))));
    }

    private static String registrar(ServerWebExchange intercambio, Throwable error) {
        String id = Incidentes.nuevoId();
        REGISTRO.error(
                "Incidente {}: {} {} falló con {}{}{}",
                id,
                intercambio.getRequest().getMethod(),
                SaneadorDeTrazas.sanear(intercambio.getRequest().getPath().value()),
                error.getClass().getName(),
                System.lineSeparator(),
                SaneadorDeTrazas.trazaSaneada(error));
        return id;
    }

    private static boolean esFallaDeConexion(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConnectException
                    || causa instanceof UnknownHostException
                    || causa instanceof NoRouteToHostException
                    || TIEMPO_DE_CONEXION.equals(causa.getClass().getName())) {
                return true;
            }
        }
        return false;
    }
}
