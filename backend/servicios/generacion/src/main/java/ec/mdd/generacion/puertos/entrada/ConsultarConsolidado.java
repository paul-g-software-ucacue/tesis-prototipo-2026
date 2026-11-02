package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;

/** El consolidado de un periodo (RF-32). */
public interface ConsultarConsolidado {

    /**
     * Lo calcula al abrirlo, sin guardar nada.
     *
     * @param periodo el código del periodo
     * @return el consolidado; vacío si no tiene distributivo
     * @throws PeriodoNoEncontrado si el periodo no existe
     */
    ConsolidadoDelPeriodo consolidado(String periodo);
}
