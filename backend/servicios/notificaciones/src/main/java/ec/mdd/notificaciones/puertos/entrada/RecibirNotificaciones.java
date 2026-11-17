package ec.mdd.notificaciones.puertos.entrada;

/**
 * Caso de uso: recibir las notificaciones que pide otro proceso, una sola vez por {@code idEvento}
 * (F16-H01, ADR-018).
 */
@FunctionalInterface
public interface RecibirNotificaciones {

    /**
     * @param evento el evento recibido
     * @throws ec.mdd.notificaciones.aplicacion.EventoNoReconocido si el tipo no es uno admitido o
     *     no coincide con el origen
     * @throws ec.mdd.notificaciones.dominio.NotificacionInvalida si el contenido no es una
     *     solicitud válida
     */
    void recibir(EventoRecibido evento);
}
