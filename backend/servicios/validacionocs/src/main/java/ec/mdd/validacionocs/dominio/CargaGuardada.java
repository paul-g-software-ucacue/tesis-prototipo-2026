package ec.mdd.validacionocs.dominio;

import java.time.Instant;

/**
 * Un archivo cargado para un periodo.
 *
 * @param numero su versión en el periodo, desde 1
 * @param contenido el archivo tal como se subió
 * @param por quién lo cargó
 * @param en cuándo
 */
public record CargaGuardada(int numero, String contenido, String por, Instant en) {}
