package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.TipoDeInconsistencia;

/**
 * Lo que pide quien justifica una inconsistencia.
 *
 * @param periodo el código del periodo
 * @param tipo el tipo de la inconsistencia
 * @param objeto la clave de lo que la incumple
 * @param texto el motivo
 * @param quien quién la justifica
 */
public record SolicitudDeJustificacion(
        String periodo, TipoDeInconsistencia tipo, String objeto, String texto, String quien) {}
