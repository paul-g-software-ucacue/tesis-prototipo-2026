package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.Solicitante;

/**
 * Caso de uso: el coordinador envía el parcial de su carrera (F12-H02, US1). Con el último, el
 * distributivo pasa a la siguiente etapa activa (US2).
 */
public interface EnviarParcial {

    /**
     * @param periodo el código del periodo
     * @param carrera la carrera del parcial
     * @param quien quien lo envía
     * @return el estado del flujo después del envío
     * @throws SinDistributivo si el periodo no tiene distributivo
     * @throws PeriodoCerrado si el periodo está cerrado
     */
    FlujoDelDistributivo enviar(String periodo, String carrera, Solicitante quien);
}
