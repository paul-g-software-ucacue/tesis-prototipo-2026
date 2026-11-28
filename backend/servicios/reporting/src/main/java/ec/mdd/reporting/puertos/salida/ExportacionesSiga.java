package ec.mdd.reporting.puertos.salida;

import ec.mdd.reporting.dominio.ExportacionSiga;
import java.util.List;

/**
 * Las exportaciones a SIGA registradas (RF-53, F19-H02, D-120). Solo se agregan: ninguna se
 * modifica ni se borra (principio VII). Se guarda dentro de la transacción de su entrada en la
 * bitácora, para que queden las dos o ninguna (FR-013).
 */
public interface ExportacionesSiga {

    /**
     * @param exportacion una exportación nueva
     */
    void guardar(ExportacionSiga exportacion);

    /**
     * @param periodo el código del periodo
     * @return sus exportaciones, de la más reciente a la más antigua; vacía si no tiene
     */
    List<ExportacionSiga> delPeriodo(String periodo);
}
