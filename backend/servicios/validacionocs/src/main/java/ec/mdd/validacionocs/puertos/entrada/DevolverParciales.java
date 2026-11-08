package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.DevolucionDeParcial;
import ec.mdd.validacionocs.dominio.Solicitante;
import java.util.List;

/**
 * Caso de uso: devolver desde EN_CONSOLIDACION el parcial de una o varias carreras, cada una con su
 * observación (F12-H05, D-98). El distributivo vuelve a BORRADOR_CARRERA; no notifica.
 */
public interface DevolverParciales {

    /**
     * @param periodo el código del periodo
     * @param devoluciones las carreras, cada una con su observación; al menos una
     * @param quien quien devuelve
     * @return el estado del flujo después de la devolución
     * @throws SinDistributivo si el periodo no tiene distributivo
     * @throws PeriodoCerrado si el periodo está cerrado
     */
    FlujoDelDistributivo devolver(
            String periodo, List<DevolucionDeParcial> devoluciones, Solicitante quien);
}
