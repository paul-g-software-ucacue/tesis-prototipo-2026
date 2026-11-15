package ec.mdd.firma.puertos.salida;

import ec.mdd.firma.puertos.entrada.CadenaDeFirmasCompleta;

/** Avisa que la cadena de firmas quedó completa (F14-H03, D-104, MD-05). */
@FunctionalInterface
public interface AvisoDeCadenaCompleta {

    /**
     * Guarda el aviso en la transacción en curso: si la transacción no confirma, no sale.
     *
     * @param aviso el periodo y la versión firmados
     */
    void avisar(CadenaDeFirmasCompleta aviso);
}
