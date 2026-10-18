package ec.mdd.plataforma.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Responde 403 cuando un token válido no basta para la petición, con el modelo de error del
 * principio XIII y el código {@code SEGURIDAD_ACCESO_NO_AUTORIZADO} (ADR-021).
 *
 * <p>La respuesta es idéntica para todas las causas: falta el permiso de la operación, un token de
 * usuario llega a {@code /interno/**} o uno de proceso llega fuera de él. No dice qué permiso
 * faltó, para no describirle a nadie la matriz de permisos. La causa solo se registra, en DEBUG.
 */
public final class RespuestaAccesoNoAutorizado implements AccessDeniedHandler {

    private static final Logger REGISTRO =
            LoggerFactory.getLogger(RespuestaAccesoNoAutorizado.class);

    private static final byte[] CUERPO =
            ("{\"titulo\":\"Acceso no autorizado\","
                            + "\"descripcion\":\"No tienes permiso para esta acción. Si crees que"
                            + " deberías tenerlo, pide al administrador que revise tus roles.\","
                            + "\"codigo\":\"SEGURIDAD_ACCESO_NO_AUTORIZADO\"}")
                    .getBytes(StandardCharsets.UTF_8);

    @Override
    public void handle(
            HttpServletRequest peticion, HttpServletResponse respuesta, AccessDeniedException causa)
            throws IOException {
        REGISTRO.debug(
                "Petición {} {} sin permiso: {}",
                peticion.getMethod(),
                peticion.getRequestURI(),
                causa.getMessage());
        respuesta.setStatus(HttpServletResponse.SC_FORBIDDEN);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setContentLength(CUERPO.length);
        respuesta.getOutputStream().write(CUERPO);
    }
}
