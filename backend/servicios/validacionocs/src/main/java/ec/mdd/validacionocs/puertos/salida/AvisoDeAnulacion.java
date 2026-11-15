package ec.mdd.validacionocs.puertos.salida;

import java.time.Instant;
import java.util.Objects;

/**
 * La orden de anular las firmas de una versión rechazada en firma, guardada en la transacción que
 * registra la versión nueva (F12-H04, D-106): si la transacción no confirma, no sale. Después de
 * confirmar, Aprobación se la entrega a Firma (MD-05), como la cadena de {@link AvisoDeFirma}.
 */
@FunctionalInterface
public interface AvisoDeAnulacion {

    /**
     * Guarda la orden en la transacción en curso.
     *
     * @param aviso el periodo, la versión rechazada y cuándo
     */
    void avisar(FirmasPorAnular aviso);

    /**
     * La versión firmada se reemplazó por un rechazo en firma: hay que anular las firmas de su
     * documento. El motivo es el rechazo en firma, el único que Aprobación ordena hoy.
     *
     * @param periodo el código del periodo
     * @param version la versión rechazada
     * @param momento cuándo se rechazó
     */
    record FirmasPorAnular(String periodo, int version, Instant momento) {

        /** Exige los datos. */
        public FirmasPorAnular {
            Objects.requireNonNull(periodo, "periodo");
            Objects.requireNonNull(momento, "momento");
        }
    }
}
