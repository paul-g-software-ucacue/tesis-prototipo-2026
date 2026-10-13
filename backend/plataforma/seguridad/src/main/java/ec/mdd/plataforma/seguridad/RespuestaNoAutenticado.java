package ec.mdd.plataforma.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Responde 401 cuando una petición no trae un token válido, con el modelo de error del principio
 * XIII y el código {@code SEGURIDAD_NO_AUTENTICADO} (D-38).
 *
 * <p>La respuesta es idéntica para todas las causas (sin token, firma alterada, vencido, otro
 * algoritmo, otra llave u otro emisor), y el encabezado {@code WWW-Authenticate} dice solo {@code
 * Bearer}: Spring Security, por defecto, agregaría {@code error_description} con la causa, y eso le
 * diría a un atacante qué falló (ADR-014). La causa solo se registra, en DEBUG y sin el token.
 */
public final class RespuestaNoAutenticado implements AuthenticationEntryPoint {

    private static final Logger REGISTRO = LoggerFactory.getLogger(RespuestaNoAutenticado.class);

    private static final byte[] CUERPO =
            ("{\"titulo\":\"Sesión no válida\","
                            + "\"descripcion\":\"Inicia sesión de nuevo para continuar.\","
                            + "\"codigo\":\"SEGURIDAD_NO_AUTENTICADO\"}")
                    .getBytes(StandardCharsets.UTF_8);

    @Override
    public void commence(
            HttpServletRequest peticion,
            HttpServletResponse respuesta,
            AuthenticationException causa)
            throws IOException {
        REGISTRO.debug(
                "Petición {} {} sin autenticación válida: {}",
                peticion.getMethod(),
                peticion.getRequestURI(),
                causa.getMessage());
        respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setContentLength(CUERPO.length);
        respuesta.getOutputStream().write(CUERPO);
    }
}
