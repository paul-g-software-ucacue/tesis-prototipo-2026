package ec.mdd.firma.puertos.entrada;

import java.time.Instant;
import java.util.UUID;

/**
 * El rechazo en firma registrado, como lo ve quien rechazó (F14-H04).
 *
 * @param documento el documento firmable
 * @param periodo el código del periodo
 * @param version la versión congelada
 * @param orden el paso que se rechazó
 * @param rotulo el rótulo del paso, como en el PDF («Revisado por»)
 * @param rechazadoPor quién rechazó
 * @param rechazadoEn cuándo
 * @param observacion la observación, sin los espacios de los extremos
 */
public record RechazoRegistrado(
        UUID documento,
        String periodo,
        int version,
        int orden,
        String rotulo,
        String rechazadoPor,
        Instant rechazadoEn,
        String observacion) {}
