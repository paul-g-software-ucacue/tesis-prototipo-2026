package ec.mdd.firma.puertos.entrada;

import java.time.Instant;
import java.util.Objects;

/**
 * La cadena de firmas de la versión congelada quedó completa: se aceptó la última firma (F14-H03,
 * D-104, glosario §6).
 *
 * <p>Firma no conoce el flujo (MD-05): solo dice que la cadena está completa. Va en la API
 * publicada para que Aprobación, que ya depende de Firma, lo escuche y decida a qué etapa pasa, sin
 * un ciclo entre los dos. Se publica en la transacción que registra la firma y se entrega después
 * de confirmarla, con reintento (ADR-018): puede llegar más de una vez.
 *
 * @param periodo el código del periodo
 * @param version la versión congelada que se firmó
 * @param momento cuándo se aceptó la última firma
 */
public record CadenaDeFirmasCompleta(String periodo, int version, Instant momento) {

    /** Exige los datos. */
    public CadenaDeFirmasCompleta {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(momento, "momento");
        if (version < 1) {
            throw new IllegalArgumentException("la versión empieza en 1");
        }
    }
}
