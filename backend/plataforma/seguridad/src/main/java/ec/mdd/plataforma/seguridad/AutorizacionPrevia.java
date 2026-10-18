package ec.mdd.plataforma.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Evalúa el {@code @PreAuthorize} de la operación antes de leer y validar la petición (ADR-021).
 *
 * <p>Spring MVC lee el cuerpo y valida los parámetros antes de llamar al método del controlador, y
 * recién ahí actúa {@code @PreAuthorize}. Sin este paso, un usuario sin el permiso que envía datos
 * inválidos recibiría 400 con el detalle de los campos, en lugar de 403. El {@code @PreAuthorize}
 * del método sigue siendo la única declaración del permiso y se vuelve a evaluar al llamarlo.
 */
final class AutorizacionPrevia implements HandlerInterceptor {

    private final PreAuthorizeAuthorizationManager autorizacion =
            new PreAuthorizeAuthorizationManager();

    @Override
    public boolean preHandle(
            HttpServletRequest peticion, HttpServletResponse respuesta, Object manejador) {
        if (manejador instanceof HandlerMethod metodo && protegido(metodo)) {
            AuthorizationResult resultado =
                    autorizacion.authorize(
                            SecurityContextHolder.getContext()::getAuthentication,
                            new SimpleMethodInvocation(metodo.getBean(), metodo.getMethod()));
            if (resultado != null && !resultado.isGranted()) {
                throw new AccessDeniedException("Sin el permiso de la operación");
            }
        }
        return true;
    }

    private static boolean protegido(HandlerMethod metodo) {
        return AnnotatedElementUtils.hasAnnotation(metodo.getMethod(), PreAuthorize.class)
                || AnnotatedElementUtils.hasAnnotation(metodo.getBeanType(), PreAuthorize.class);
    }
}
