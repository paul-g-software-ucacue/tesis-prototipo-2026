package ec.mdd.validacionocs.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * El vencimiento del plazo de la etapa actual de un distributivo (`PlazoDeEtapaVencido` del
 * glosario, F12-H03, D-114). Hay uno por llegada a la etapa: es lo que impide repetir el aviso
 * (aclaración del 2026-11-20).
 *
 * @param periodo el periodo del distributivo
 * @param plazo la etapa, la llegada, los días y la fecha límite
 * @param detectadoEn cuándo lo detectó la revisión
 * @param diasDeAtraso los días desde la fecha límite, 1 o más
 */
public record VencimientoDePlazo(
        String periodo, Plazo plazo, Instant detectadoEn, int diasDeAtraso) {

    /** Exige todo y al menos un día de atraso: el día de la fecha límite todavía no vence. */
    public VencimientoDePlazo {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(plazo, "plazo");
        Objects.requireNonNull(detectadoEn, "detectadoEn");
        if (diasDeAtraso < 1) {
            throw new IllegalArgumentException("Un vencimiento lleva al menos un día de atraso");
        }
    }
}
