package ec.mdd.normativa.puertos.entrada;

import ec.mdd.normativa.dominio.PesosAfinidad;

/**
 * Los pesos de afinidad de la versión que rige un periodo.
 *
 * @param version el número de la versión
 * @param pesos sus pesos
 */
public record PesosDeLaVersion(int version, PesosAfinidad pesos) {}
