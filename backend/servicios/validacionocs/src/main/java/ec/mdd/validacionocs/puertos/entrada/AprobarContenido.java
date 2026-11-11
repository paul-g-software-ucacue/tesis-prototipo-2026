package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.Solicitante;

/**
 * Caso de uso: aprobar el contenido del distributivo y congelar su versión (F13-H01, D-100). Pasa
 * de EN_REVISION_VRA a CONGELADO_EN_FIRMA; sin comentario y sin notificar.
 *
 * <p>Son dos transacciones, una por servicio, como enviar o devolver un parcial (principio V,
 * research §1): Generación, dueña de la versión, la congela en la suya; después Aprobación, dueña
 * de la etapa, registra el paso con la versión y el hash que devolvió. Congelar es idempotente: si
 * la segunda transacción falla, volver a aprobar la completa con el mismo hash, sin compensar nada.
 * Antes de tocar Generación, el dominio rechaza lo que no cabe (FR-009).
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
