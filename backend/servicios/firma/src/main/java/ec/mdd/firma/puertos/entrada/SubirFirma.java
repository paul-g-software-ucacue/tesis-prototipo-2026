package ec.mdd.firma.puertos.entrada;

import ec.mdd.firma.dominio.ResultadoValidacion;
import java.util.UUID;

/**
 * Sube el PDF firmado del paso en turno y lo valida (F14-H02, RF-39, D-23, D-103).
 *
 * <p>Solo el firmante en turno: a cualquier otro, y por un documento que no existe o no está
 * generado, {@link ec.mdd.firma.dominio.SinTurnoDeFirma}, sin resultado ni registro. Al firmante en
 * turno, cada subida le deja su resultado y su entrada en la bitácora. Si se acepta, el archivo
 * pasa a ser el vigente y el turno avanza; si no, el archivo no se guarda.
 */
public interface SubirFirma {

    /**
     * Valida el archivo y, si pasa, registra la firma.
     *
     * @param documento el documento firmable
     * @param archivo el PDF firmado, de 1 byte a 10 MB
     * @param quien quien lo sube
     * @return el resultado aceptado
     * @throws ec.mdd.firma.dominio.SinTurnoDeFirma si no es el firmante en turno
     * @throws ec.mdd.firma.dominio.ArchivoVacio si no tiene bytes
     * @throws ec.mdd.firma.dominio.ArchivoDemasiadoGrande si pasa de 10 MB
     * @throws ec.mdd.firma.dominio.ArchivoNoDisponible si el vigente falta o no coincide
     * @throws ec.mdd.firma.dominio.ArchivoFirmadoRechazado si no pasa la validación, con su
     *     resultado ya registrado
     */
    ResultadoValidacion subir(UUID documento, byte[] archivo, Solicitante quien);
}
