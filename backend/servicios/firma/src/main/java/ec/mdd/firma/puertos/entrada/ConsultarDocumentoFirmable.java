package ec.mdd.firma.puertos.entrada;

/** Caso de uso: el estado del documento firmable del periodo (F13-H02, FR-014). */
public interface ConsultarDocumentoFirmable {

    /**
     * @param periodo el código del periodo
     * @param quien quien consulta, para sus acciones
     * @return el estado; SIN_GENERAR sin versión si Aprobación todavía no lo pidió
     */
    EstadoDelDocumento estado(String periodo, Solicitante quien);
}
