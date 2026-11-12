package ec.mdd.firma.puertos.entrada;

/**
 * Caso de uso: reintentar el documento firmable que no se pudo generar al entrar en firma (F13-H02,
 * FR-012). Solo el rol de la etapa que llegó con la solicitud. Idempotente: con el documento ya
 * generado, responde el mismo.
 */
public interface GenerarDocumentoFirmable {

    /**
     * @param periodo el código del periodo
     * @param quien quien lo pide
     * @return el estado, GENERADO
     * @throws ec.mdd.firma.dominio.EtapaNoAdmiteGeneracion si Aprobación no pidió el documento
     * @throws ec.mdd.firma.dominio.SinRolDeFirma si no tiene el rol de la etapa
     * @throws ec.mdd.firma.dominio.GeneracionFallida si la generación falla, con el motivo
     */
    EstadoDelDocumento generar(String periodo, Solicitante quien);
}
