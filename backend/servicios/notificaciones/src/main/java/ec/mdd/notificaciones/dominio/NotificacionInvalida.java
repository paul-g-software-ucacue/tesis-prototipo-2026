package ec.mdd.notificaciones.dominio;

/**
 * Una solicitud de notificaciones que no cumple su lenguaje publicado ({@code
 * SolicitudDeNotificaciones} de {@code contratos/notificaciones}). Se traduce a {@code
 * NOTIFICACIONES_NOTIFICACION_INVALIDA} (400): no se registra nada, y el emisor deja la publicación
 * pendiente (ADR-018). El mensaje dice qué falló, nunca el contenido.
 */
public final class NotificacionInvalida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Error con el motivo. */
    public NotificacionInvalida(String motivo) {
        super(motivo);
    }
}
