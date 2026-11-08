package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.Solicitante;

/** Caso de uso: ver el estado del flujo del distributivo y lo que puede hacer quien consulta. */
public interface ConsultarFlujo {

    /**
     * @param periodo el código del periodo
     * @param quien quien consulta
     * @return el estado del flujo
     * @throws SinDistributivo si el periodo no tiene distributivo
     */
    FlujoDelDistributivo flujo(String periodo, Solicitante quien);
}
