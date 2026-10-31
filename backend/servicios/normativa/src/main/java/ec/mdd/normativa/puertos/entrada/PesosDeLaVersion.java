package ec.mdd.normativa.puertos.entrada;

import ec.mdd.normativa.dominio.MinimoAfinidad;
import ec.mdd.normativa.dominio.PesosAfinidad;

/**
 * Los pesos y el mínimo de afinidad de la versión que rige un periodo.
 *
 * @param version el número de la versión
 * @param pesos sus pesos
 * @param minimo su mínimo de afinidad
 */
public record PesosDeLaVersion(int version, PesosAfinidad pesos, MinimoAfinidad minimo) {}
