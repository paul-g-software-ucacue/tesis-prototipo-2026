package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.Solicitante;

/**
 * Caso de uso: el avance genérico de una etapa opcional sin historia propia, hoy BORRADOR_UNIDADES
 * (F12-H02, US3).
 */
public interface AvanzarEtapa {

    /**
     * @param periodo el código del periodo
     * @param quien quien avanza
     * @return el estado del flujo después del avance
     * @throws SinDistributivo si el periodo no tiene distributivo
     * @throws PeriodoCerrado si el periodo está cerrado
     */
    FlujoDelDistributivo avanzar(String periodo, Solicitante quien);
}
