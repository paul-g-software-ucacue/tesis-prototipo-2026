package ec.mdd.reporting.puertos.salida;

import ec.mdd.reporting.dominio.DescargaDeReporte;

/**
 * Registra cada descarga en la bitácora (F20-H01, D-115, ADR-018). Se llama dentro de una
 * transacción: el evento se guarda en el registro de publicaciones del proceso y se entrega a
 * Usuarios después de confirmarla.
 */
public interface RegistroDeDescargas {

    /**
     * @param descarga la descarga
     */
    void registrar(DescargaDeReporte descarga);
}
