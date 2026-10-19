package ec.mdd.normativa.dominio;

import java.time.Instant;

/**
 * Evento de dominio: se creó una versión de normativa a partir de otra (modelo de dominio §6). Hoy
 * queda en el registro como {@code VERSION_CREADA}; se publicará a otros procesos cuando tenga un
 * consumidor (principio X).
 *
 * @param numero el número de la versión nueva
 * @param codigo su código
 * @param copiadaDe el número de la versión copiada
 * @param momento cuándo se creó
 */
public record VersionNormativaCreada(int numero, String codigo, int copiadaDe, Instant momento) {}
