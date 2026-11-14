package ec.mdd.firma.puertos.entrada;

import java.util.List;

/**
 * Los documentos firmables que le toca firmar a quien pregunta (F14-H01, D-102): los generados cuyo
 * paso en turno tiene como firmante esperado a esa persona. Tener el rol del paso no basta.
 */
public interface ConsultarFirmasPendientes {

    /**
     * @param quien el usuario y sus roles, del token
     * @return sus firmas pendientes, por periodo y versión; vacía si no tiene
     */
    List<FirmaPendiente> pendientes(Solicitante quien);
}
