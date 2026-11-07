package ec.mdd.plataforma.eventos;

/**
 * Un receptor de eventos del mismo proceso, que {@link ClienteDeEventos} llama sin HTTP ni token de
 * proceso (ADR-024).
 *
 * <p>Dentro de un proceso, los servicios se llaman por su interfaz de aplicación (principio V). La
 * publicación sigue en el registro del proceso y el reenvío no cambia: si {@link #recibir} lanza
 * una excepción, la entrega es una {@link EntregaFallida} y se repite como cualquier otra.
 */
public interface ReceptorLocal {

    /**
     * El servicio destino que atiende, con el mismo nombre que en {@code mdd.eventos.destinos}.
     *
     * @return por ejemplo {@code usuarios}
     */
    String destino();

    /**
     * Registra el evento en la transacción del servicio receptor. Debe ser idempotente por {@code
     * idEvento}, como un receptor HTTP (ADR-018).
     *
     * @param evento el sobre guardado en el registro
     */
    void recibir(EventoEntreProcesos evento);
}
