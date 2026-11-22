package ec.mdd.reporting.puertos.salida;

import ec.mdd.reporting.dominio.CsvGenerado;
import ec.mdd.reporting.dominio.DescargaDeReporte;
import ec.mdd.reporting.dominio.LayoutCargado;

/**
 * Registra en la bitácora lo que hace Reporting: cada descarga de un reporte (F20-H01, D-115), cada
 * CSV para SIGA generado y cada carga del layout de exportación (F19-H01, D-119). Se llama dentro
 * de una transacción: el evento se guarda en el registro de publicaciones del proceso y se entrega
 * a Usuarios después de confirmarla (ADR-018).
 */
public interface RegistroEnBitacora {

    /**
     * @param descarga la descarga
     */
    void registrar(DescargaDeReporte descarga);

    /**
     * @param generado un CSV para SIGA, antes de entregarlo (FR-021)
     */
    void registrar(CsvGenerado generado);

    /**
     * @param cargado una versión nueva del layout de exportación (FR-017)
     */
    void registrar(LayoutCargado cargado);
}
