package ec.mdd.firma.puertos.entrada;

import java.util.UUID;

/**
 * El archivo vigente de un documento firmable, solo para el firmante en turno (F14-H01, D-102).
 * Antes de entregarlo se recalcula su SHA-256: nunca se entrega un archivo distinto del registrado.
 */
public interface DescargarArchivoVigente {

    /**
     * @param documento el identificador del documento
     * @param quien el usuario y sus roles, del token
     * @return el archivo verificado
     * @throws ec.mdd.firma.dominio.SinTurnoDeFirma si no es el firmante en turno, o si el documento
     *     no existe o no está generado: el mismo 403, sin decir cuál
     * @throws ec.mdd.firma.dominio.ArchivoNoDisponible si el archivo falta o no coincide
     */
    ArchivoVigente archivo(UUID documento, Solicitante quien);
}
