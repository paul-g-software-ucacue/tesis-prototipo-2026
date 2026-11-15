package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.RechazoDelFirmante;

/**
 * El firmante en turno rechazó en firma: el distributivo vuelve de CONGELADO_EN_FIRMA a la etapa
 * activa anterior (F14-H04, D-105). Lo pide el aviso de Firma, que puede llegar más de una vez.
 */
public interface RegistrarRechazoEnFirma {

    /**
     * Devuelve el distributivo del periodo si está en CONGELADO_EN_FIRMA con esa versión; si no, no
     * hace nada y no falla, para que el aviso no se reintente para siempre.
     *
     * @param periodo el código del periodo
     * @param version la versión rechazada
     * @param rechazo quién rechazó, en qué paso, cuándo y por qué
     */
    void registrar(String periodo, int version, RechazoDelFirmante rechazo);
}
