package ec.mdd.generacion.puertos.entrada;

/**
 * La versión nueva que creó Generación por orden de Aprobación, tras anular la congelada (F12-H04,
 * D-106).
 *
 * @param numero el número de la versión nueva
 * @param hash el hash del distributivo de esa versión, el SHA-256 en hexadecimal, el mismo que da
 *     {@link ParcialesDelDistributivo#hashDelDistributivo}
 */
public record VersionNuevaEnGeneracion(int numero, String hash) {}
