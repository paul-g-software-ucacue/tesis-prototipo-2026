package ec.mdd.normativa.puertos.entrada;

import java.time.Instant;

/**
 * La versión de normativa que rige un periodo y desde cuándo: su asignación normativa (glosario
 * §7.3).
 *
 * @param periodo el código del periodo
 * @param version el número de la versión
 * @param asociadaEn cuándo se asoció
 */
public record AsignacionDelPeriodo(String periodo, int version, Instant asociadaEn) {}
