package ec.mdd.generacion.puertos.entrada;

/**
 * Un parcial marcado como enviado (F12-H02).
 *
 * @param version el número de la versión vigente
 * @param hash el SHA-256 del contenido del parcial, en hexadecimal
 */
public record ParcialMarcado(int version, String hash) {}
