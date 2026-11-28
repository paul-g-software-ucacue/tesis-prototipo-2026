package ec.mdd.reporting.puertos.entrada;

import ec.mdd.reporting.dominio.ExportacionSiga;
import java.util.List;

/**
 * Las exportaciones a SIGA de un periodo, para la Secretaría general académica (F19-H02, FR-018).
 */
public interface ConsultarExportacionesSiga {

    /**
     * @param periodo el código del periodo
     * @return sus exportaciones, de la más reciente a la más antigua; vacía si no tiene
     */
    List<ExportacionSiga> delPeriodo(String periodo);
}
