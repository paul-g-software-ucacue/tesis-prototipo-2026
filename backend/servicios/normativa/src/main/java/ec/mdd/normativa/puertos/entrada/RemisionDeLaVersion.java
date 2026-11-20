package ec.mdd.normativa.puertos.entrada;

/**
 * Los días antes del inicio del periodo en que se remite el distributivo aprobado por el OCS, de la
 * versión que rige un periodo (RN-23, POL 5.2).
 *
 * @param version el número de la versión
 * @param diasAntesDelInicio los días
 */
public record RemisionDeLaVersion(int version, int diasAntesDelInicio) {}
