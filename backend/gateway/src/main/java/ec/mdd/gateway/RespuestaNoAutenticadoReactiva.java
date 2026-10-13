package ec.mdd.gateway;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Responde 401 en el API Gateway cuando una petición a la API no trae un token válido, con el mismo
 * cuerpo y el mismo encabezado que los procesos: {@code SEGURIDAD_NO_AUTENTICADO} y {@code
 * WWW-Authenticate: Bearer}, sin decir qué falló (D-38, ADR-014). La causa solo se registra, en
 * DEBUG y sin el token.
 */
final class RespuestaNoAutenticadoReactiva implements ServerAuthenticationEntryPoint {

    private static final Logger REGISTRO =
            LoggerFactory.getLogger(RespuestaNoAutenticadoReactiva.class);

    private static final byte[] CUERPO =
            ("{\"titulo\":\"Sesión no válida\","
                            + "\"descripcion\":\"Inicia sesión de nuevo para continuar.\","
                            + "\"codigo\":\"SEGURIDAD_NO_AUTENTICADO\"}")
                    .getBytes(StandardCharsets.UTF_8);

    @Override
    public Mono<Void> commence(ServerWebExchange intercambio, AuthenticationException causa) {
        REGISTRO.debug(
                "Petición {} {} sin autenticación válida: {}",
                intercambio.getRequest().getMethod(),
                intercambio.getRequest().getPath(),
                causa.getMessage());
        ServerHttpResponse respuesta = intercambio.getResponse();
        respuesta.setStatusCode(HttpStatus.UNAUTHORIZED);
        respuesta.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return respuesta.writeWith(Mono.just(respuesta.bufferFactory().wrap(CUERPO)));
    }
}
