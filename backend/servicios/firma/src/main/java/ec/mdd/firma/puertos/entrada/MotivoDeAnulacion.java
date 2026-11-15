package ec.mdd.firma.puertos.entrada;

/**
 * Por qué se anulan las firmas de un documento (F12-H04, D-106). Es el valor del contrato ({@code
 * AnulacionDeFirmas.motivo}); está en la interfaz publicada porque lo usa Aprobación al ordenar la
 * anulación (MD-05, principio V).
 */
public enum MotivoDeAnulacion {
    /** El firmante en turno rechazó en firma y se creó una versión nueva del distributivo. */
    RECHAZO_EN_FIRMA
}
