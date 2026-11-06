package ec.mdd.validacionocs.puertos.salida;

/** La bitácora de los cambios de Validación OCS (F17-H01, principio VII). */
public interface RegistroDeCambios {

    /**
     * Registra un cambio en la transacción en curso: si se revierte, el registro también.
     *
     * @param cambio el cambio ya guardado
     */
    void registrar(CambioDeFlujo cambio);
}
