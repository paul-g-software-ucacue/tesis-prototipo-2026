package ec.mdd.plataforma.eventos;

/**
 * Una entrega que el receptor no confirmó: no respondió, venció el plazo, respondió con un error o
 * no se pudo obtener el token de proceso. El oyente la vuelve a lanzar para que la publicación
 * quede {@code FAILED} y la tarea programada la reenvíe (ADR-018).
 *
 * <p>Su mensaje dice el estado HTTP o la clase de la causa; nunca el contenido del evento, la clave
 * del proceso ni el token.
 */
public final class EntregaFallida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Entrega fallida con un motivo seguro para el registro. */
    public EntregaFallida(String motivo) {
        super(motivo);
    }
}
