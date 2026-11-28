package ec.mdd.reporting.puertos.salida;

import ec.mdd.reporting.dominio.DescargaDeReporte;
import ec.mdd.reporting.dominio.ExportacionSiga;
import ec.mdd.reporting.dominio.LayoutCargado;
import ec.mdd.reporting.dominio.RechazoDeExportacion;

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
     * Un CSV para SIGA entregado: una sola entrada por exportación, con su suma de verificación
     * (F19-H02, FR-014). Se llama en la misma transacción que guarda la exportación.
     *
     * @param exportacion la exportación, antes de entregar el archivo
     */
    void registrar(ExportacionSiga exportacion);

    /**
     * @param cargado una versión nueva del layout de exportación (FR-017)
     */
    void registrar(LayoutCargado cargado);

    /**
     * @param rechazo un CSV para SIGA rechazado por firmas o inconsistencias (F19-H02, FR-016)
     */
    void registrar(RechazoDeExportacion rechazo);
}
