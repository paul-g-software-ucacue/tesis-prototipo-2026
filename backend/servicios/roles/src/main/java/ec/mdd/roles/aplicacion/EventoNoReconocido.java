package ec.mdd.roles.aplicacion;

/**
 * El receptor no reconoce el tipo del evento, o su contenido. Se traduce a {@code
 * COMUN_EVENTO_NO_RECONOCIDO} (400), y el emisor deja la publicación pendiente (ADR-018).
 */
public final class EventoNoReconocido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Error con el tipo recibido, que no es un dato personal ni lleva el contenido. */
    public EventoNoReconocido(String tipo) {
        super("Sin manejador para el tipo de evento " + tipo);
    }
}
