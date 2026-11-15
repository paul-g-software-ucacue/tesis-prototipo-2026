package ec.mdd.firma.puertos.entrada;

import java.time.Instant;
import java.util.Objects;

/**
 * Las firmas del documento de una versión quedaron anuladas (F12-H04, D-106, glosario §6). Firma lo
 * publica en la transacción de la anulación. Todavía no lo escucha nadie: es para las
 * notificaciones de F16.
 *
 * @param periodo el código del periodo
 * @param version la versión cuyo documento se anuló
 * @param anuladas cuántas firmas aceptadas tenía
 * @param motivo por qué
 * @param momento cuándo
 */
public record FirmasAnuladas(
        String periodo, int version, int anuladas, MotivoDeAnulacion motivo, Instant momento) {

    /** Exige los datos. */
    public FirmasAnuladas {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(motivo, "motivo");
        Objects.requireNonNull(momento, "momento");
        if (version < 1 || anuladas < 0) {
            throw new IllegalArgumentException("la versión empieza en 1 y las firmas en 0");
        }
    }
}
