package ec.mdd.firma.puertos.salida;

import ec.mdd.firma.puertos.entrada.RechazoEnFirma;

/** Avisa que el firmante en turno rechazó en firma (F14-H04, D-105, MD-05). */
@FunctionalInterface
public interface AvisoDeRechazoEnFirma {

    /**
     * Guarda el aviso en la transacción en curso: si la transacción no confirma, no sale.
     *
     * @param aviso el periodo, la versión, el paso, quién y la observación
     */
    void avisar(RechazoEnFirma aviso);
}
