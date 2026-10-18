package ec.mdd.gateway;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Responde 403 en el API Gateway con el mismo cuerpo que los procesos, {@code
 * SEGURIDAD_ACCESO_NO_AUTORIZADO} (ADR-021): por ejemplo, un token válido en una ruta fuera de
 * {@code /api/**}, como {@code /interno/**} (ADR-018). No dice qué faltó; la causa solo se
 * registra, en DEBUG.
 */
final class RespuestaAccesoNoAutorizadoReactiva implements ServerAccessDeniedHandler {

    private static final Logger REGISTRO =
            LoggerFactory.getLogger(RespuestaAccesoNoAutorizadoReactiva.class);

    private static final byte[] CUERPO =
            ("{\"titulo\":\"Acceso no autorizado\","
                            + "\"descripcion\":\"No tienes permiso para esta acción. Si crees que"
                            + " deberías tenerlo, pide al administrador que revise tus roles.\","
                            + "\"codigo\":\"SEGURIDAD_ACCESO_NO_AUTORIZADO\"}")
                    .getBytes(StandardCharsets.UTF_8);

    @Override
    public Mono<Void> handle(ServerWebExchange intercambio, AccessDeniedException causa) {
        REGISTRO.debug(
                "Petición {} {} sin permiso: {}",
                intercambio.getRequest().getMethod(),
                intercambio.getRequest().getPath(),
                causa.getMessage());
        ServerHttpResponse respuesta = intercambio.getResponse();
        respuesta.setStatusCode(HttpStatus.FORBIDDEN);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return respuesta.writeWith(Mono.just(respuesta.bufferFactory().wrap(CUERPO)));
    }
}
