package ec.mdd.generacion.puertos.entrada;

/**
 * La versión vigente congelada por orden de Aprobación (F13-H01, D-100).
 *
 * @param version el número de la versión
 * @param hash el hash del distributivo de esa versión, el SHA-256 en hexadecimal, el mismo que da
 *     {@link ParcialesDelDistributivo#hashDelDistributivo}
 */
public record VersionCongeladaEnGeneracion(int version, String hash) {}
