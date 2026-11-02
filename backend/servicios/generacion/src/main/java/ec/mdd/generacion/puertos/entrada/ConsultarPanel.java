package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.PanelDeInconsistencias;

/** El panel de inconsistencias de un periodo (RF-29). */
public interface ConsultarPanel {

    /**
     * Evalúa la versión vigente, sin guardar nada.
     *
     * @param periodo el código del periodo
     * @return el panel; vacío si el periodo no tiene distributivo
     * @throws PeriodoNoEncontrado si el periodo no existe
     * @throws PeriodoSinAbrir si no tiene versión de normativa
     */
    PanelDeInconsistencias panel(String periodo);
}
