package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;

/** Consolidar la versión vigente de un periodo (RF-32). */
public interface Consolidar {

    /**
     * Guarda quién y cuándo, sin bloquear nada.
     *
     * @param periodo el código del periodo
     * @param quien quién consolida
     * @return el consolidado
     * @throws SinDistributivo si el periodo no tiene distributivo
     * @throws PeriodoCerrado si el periodo se cerró
     */
    ConsolidadoDelPeriodo consolidar(String periodo, String quien);
}
