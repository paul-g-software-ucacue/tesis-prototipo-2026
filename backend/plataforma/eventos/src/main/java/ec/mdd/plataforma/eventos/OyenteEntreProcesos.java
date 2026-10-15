package ec.mdd.plataforma.eventos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * El oyente del mismo proceso que entrega cada evento entre procesos (principio V, ADR-018).
 *
 * <p>Con {@link ApplicationModuleListener} corre después de confirmar la transacción que publicó,
 * en otro hilo y en una transacción nueva: la transacción del servicio no espera la entrega ni
 * depende de ella. Si la entrega se confirma, Spring Modulith marca la publicación como {@code
 * COMPLETED}; si falla, la excepción la deja {@code FAILED} y {@link ReenvioDePublicaciones} la
 * vuelve a enviar.
 */
public class OyenteEntreProcesos {

    private static final Logger LOG = LoggerFactory.getLogger(OyenteEntreProcesos.class);

    private final ClienteDeEventos cliente;

    /** Oyente que entrega con el cliente dado. */
    public OyenteEntreProcesos(ClienteDeEventos cliente) {
        this.cliente = cliente;
    }

    /**
     * Entrega el evento al receptor de su destino.
     *
     * @param evento el sobre publicado
     * @throws EntregaFallida si el receptor no confirma; queda en el registro sin el contenido
     */
    @ApplicationModuleListener
    public void entregar(EventoEntreProcesos evento) {
        try {
            cliente.entregar(evento);
        } catch (EntregaFallida fallo) {
            LOG.warn(
                    "Entrega fallida del evento {} de tipo {} hacia {}: {}",
                    evento.idEvento(),
                    evento.tipo(),
                    evento.destino(),
                    fallo.getMessage());
            throw fallo;
        }
    }
}
