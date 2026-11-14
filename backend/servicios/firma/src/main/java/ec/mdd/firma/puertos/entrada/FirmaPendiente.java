package ec.mdd.firma.puertos.entrada;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un documento firmable cuyo paso en turno es de quien pregunta (F14-H01).
 *
 * @param documento el identificador del documento
 * @param periodo el periodo
 * @param version la versión congelada
 * @param orden el orden del paso en turno
 * @param rotulo el rótulo del paso, como en el PDF («Elaborado por»)
 * @param generadoEn cuándo se generó el documento
 */
public record FirmaPendiente(
        UUID documento, String periodo, int version, int orden, String rotulo, Instant generadoEn) {

    /** Exige los datos. */
    public FirmaPendiente {
        Objects.requireNonNull(documento, "documento");
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(rotulo, "rotulo");
        Objects.requireNonNull(generadoEn, "generadoEn");
    }
}
