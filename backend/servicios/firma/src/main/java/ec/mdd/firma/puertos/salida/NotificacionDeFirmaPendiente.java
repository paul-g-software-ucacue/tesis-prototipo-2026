package ec.mdd.firma.puertos.salida;

/**
 * Pide la notificación de firma pendiente para el firmante esperado del paso siguiente (F16-H02,
 * D-110). Se llama en la transacción que registra la firma aceptada: si la firma se revierte, la
 * notificación tampoco sale, y si se confirma, no se pierde (ADR-018).
 */
@FunctionalInterface
public interface NotificacionDeFirmaPendiente {

    /**
     * @param solicitud la firma aceptada y el paso siguiente
     */
    void notificar(FirmaPendienteSolicitada solicitud);
}
