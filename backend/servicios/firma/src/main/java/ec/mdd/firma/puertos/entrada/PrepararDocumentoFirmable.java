package ec.mdd.firma.puertos.entrada;

/**
 * Aprobación pide el documento firmable de la versión congelada y le entrega la cadena de firmas
 * (MD-05, F13-H02, D-101). Así Firma no conoce el flujo, y sus avisos de F14 no forman un ciclo.
 *
 * <p>Se llama después de confirmar el paso a CONGELADO_EN_FIRMA, fuera de esa transacción. Es
 * idempotente: una versión con su documento no se genera otra vez. Un fallo de la generación queda
 * registrado con su motivo y no se relanza, para que el reenvío de publicaciones no la repita sin
 * fin; el reintento lo pide el rol de la etapa.
 */
public interface PrepararDocumentoFirmable {

    /**
     * @param solicitud la versión, su hash, la cadena y el rol de la etapa
     */
    void preparar(SolicitudDeDocumento solicitud);
}
