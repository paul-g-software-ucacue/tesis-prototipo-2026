package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.Solicitante;

/**
 * Caso de uso: aprobar el contenido del distributivo y congelar su versión (F13-H01, D-100). Pasa
 * de EN_REVISION_VRA a CONGELADO_EN_FIRMA; sin comentario y sin notificar.
 */
public interface AprobarContenido {

    /**
     * @param periodo el código del periodo
     * @param quien quien aprueba
     * @return el estado del flujo después de aprobar, con la congelación
     * @throws SinDistributivo si el periodo no tiene distributivo
     * @throws PeriodoCerrado si el periodo está cerrado
     */
    FlujoDelDistributivo aprobarContenido(String periodo, Solicitante quien);
}
