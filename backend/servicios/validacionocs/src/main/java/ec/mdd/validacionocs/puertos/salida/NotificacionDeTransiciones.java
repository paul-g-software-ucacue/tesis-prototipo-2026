package ec.mdd.validacionocs.puertos.salida;

import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.Transicion;
import java.util.List;

/**
 * Pide a Notificaciones los mensajes de cada transición nueva, para quien le toca (F16-H01, RF-41,
 * D-108). Se llama en la misma transacción que guarda las transiciones: si se revierte, no queda
 * nada que entregar, y si se confirma, la entrega no se pierde aunque el proceso {@code identidad}
 * esté caído (ADR-018, MD-03).
 */
@FunctionalInterface
public interface NotificacionDeTransiciones {

    /**
     * @param proceso el proceso, ya cambiado, con la copia de la configuración
     * @param nuevas las transiciones que agregó este cambio; vacía no hace nada
     */
    void notificar(ProcesoAprobacion proceso, List<Transicion> nuevas);
}
