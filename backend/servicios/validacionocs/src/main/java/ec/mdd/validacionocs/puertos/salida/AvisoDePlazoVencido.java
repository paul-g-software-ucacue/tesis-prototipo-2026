package ec.mdd.validacionocs.puertos.salida;

import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.VencimientoDePlazo;

/**
 * Pide a Notificaciones el aviso de un plazo vencido (F12-H03, D-114, D-108). Se llama en la
 * transacción que registra el vencimiento: si se revierte, no queda nada que entregar, y si se
 * confirma, la entrega no se pierde aunque el proceso {@code identidad} esté caído (MD-03).
 */
@FunctionalInterface
public interface AvisoDePlazoVencido {

    /**
     * @param proceso el proceso, con la copia de la configuración y sus parciales
     * @param vencimiento el vencimiento recién registrado
     */
    void avisar(ProcesoAprobacion proceso, VencimientoDePlazo vencimiento);
}
