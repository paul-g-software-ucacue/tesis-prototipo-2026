package ec.mdd.firma.puertos.salida;

import ec.mdd.firma.puertos.entrada.FirmasAnuladas;

/** El aviso de firmas anuladas, en la transacción de la anulación (F12-H04, D-106). */
@FunctionalInterface
public interface AvisoDeFirmasAnuladas {

    /**
     * Guarda el aviso en la transacción en curso: si la transacción no confirma, no sale.
     *
     * @param aviso el periodo, la versión, cuántas firmas, el motivo y cuándo
     */
    void avisar(FirmasAnuladas aviso);
}
