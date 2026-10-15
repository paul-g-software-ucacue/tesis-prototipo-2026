package ec.mdd.usuarios.puertos.entrada;

/**
 * Registra un evento de otro proceso una sola vez por su identificador y aplica su efecto en la
 * misma transacción (principio V, ADR-018).
 */
public interface RegistrarEventoRecibido {

    /**
     * Registra el evento si es nuevo; si ya estaba registrado, no hace nada.
     *
     * @param evento el evento recibido
     * @throws ec.mdd.usuarios.aplicacion.EventoNoReconocido si su tipo no tiene manejador
     */
    void registrar(EventoRecibido evento);
}
