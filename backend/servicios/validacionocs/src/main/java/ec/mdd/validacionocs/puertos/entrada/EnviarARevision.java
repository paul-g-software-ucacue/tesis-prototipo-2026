package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.Solicitante;

/**
 * Caso de uso: enviar el consolidado a revisión (F10-H03, D-97). Pasa de EN_CONSOLIDACION a la
 * siguiente etapa activa si se cumplen las condiciones; no notifica.
 */
public interface EnviarARevision {

    /**
     * @param periodo el código del periodo
     * @param quien quien envía
     * @return el estado del flujo después del envío
     * @throws SinDistributivo si el periodo no tiene distributivo
     * @throws PeriodoCerrado si el periodo está cerrado
     */
    FlujoDelDistributivo enviarARevision(String periodo, Solicitante quien);
}
