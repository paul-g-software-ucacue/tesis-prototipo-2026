package ec.mdd.generacion.puertos.salida;

/** La bitácora de los cambios del distributivo (principio VII). */
public interface RegistroDeCambios {

    /**
     * Guarda el cambio y lo envía a la bitácora de Usuarios (F17-H01). Se llama dentro de la
     * transacción del cambio: si se revierte, el registro también; sin transacción, la entrada de
     * la bitácora no se publica. Un guardado que no cambia nada no llega aquí: el caso de uso no
     * guarda ni registra (D-94).
     *
     * @param cambio el cambio ya validado; nunca con secretos
     */
    void registrar(CambioDeDistributivo cambio);
}
