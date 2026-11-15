package ec.mdd.validacionocs.puertos.entrada;

import java.time.Instant;

/**
 * La cadena de firmas de la versión congelada quedó completa: el distributivo pasa a EN_OCS
 * (F14-H03, D-104). Lo pide el aviso de Firma, que puede llegar más de una vez.
 */
public interface CompletarFirmas {

    /**
     * Pasa el distributivo del periodo a EN_OCS si está en CONGELADO_EN_FIRMA con esa versión; si
     * no, no hace nada y no falla, para que el aviso no se reintente para siempre.
     *
     * @param periodo el código del periodo
     * @param version la versión firmada
     * @param momento cuándo se aceptó la última firma
     */
    void completar(String periodo, int version, Instant momento);
}
