package ec.mdd.notificaciones.dominio;

/**
 * La notificación no existe o es de otro usuario: las dos se responden igual, con {@code
 * NOTIFICACIONES_NOTIFICACION_NO_ENCONTRADA} (404), para no revelar que existe (principio IX).
 */
public final class NotificacionNoEncontrada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Error sin el identificador ni el usuario. */
    public NotificacionNoEncontrada() {
        super("La notificación no existe o es de otro usuario");
    }
}
