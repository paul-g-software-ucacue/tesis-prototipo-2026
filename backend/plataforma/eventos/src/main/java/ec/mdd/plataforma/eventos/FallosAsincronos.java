package ec.mdd.plataforma.eventos;

import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;

/**
 * Las excepciones de los métodos asíncronos del proceso, como el oyente de eventos.
 *
 * <p>Una {@link EntregaFallida} ya quedó en el registro como WARN (oyente) y como INFO (Spring
 * Modulith deja la publicación pendiente); aquí baja a DEBUG, para no repetir su traza en ERROR en
 * cada reenvío mientras el receptor está caído. Cualquier otra excepción se registra como siempre,
 * en ERROR y con su traza.
 */
final class FallosAsincronos implements AsyncUncaughtExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FallosAsincronos.class);

    private final AsyncUncaughtExceptionHandler porDefecto =
            new SimpleAsyncUncaughtExceptionHandler();

    @Override
    public void handleUncaughtException(Throwable fallo, Method metodo, Object... parametros) {
        if (fallo instanceof EntregaFallida) {
            LOG.debug("Entrega fallida en {}: {}", metodo.getName(), fallo.getMessage());
            return;
        }
        porDefecto.handleUncaughtException(fallo, metodo, parametros);
    }
}
