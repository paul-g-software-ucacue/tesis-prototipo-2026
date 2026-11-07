package ec.mdd.docentes.puertos.salida;

/** Guarda cada cambio de los docentes, para la bitácora (FR-010, principio VII). */
public interface RegistroDeCambios {

    /**
     * Guarda el cambio y lo envía a la bitácora de Usuarios (F17-H01). Se llama dentro de la
     * transacción del cambio: si se revierte, el registro también; sin transacción, la entrada de
     * la bitácora no se publica.
     *
     * @param cambio el cambio ya validado; nunca con secretos
     */
    void registrar(CambioRegistrado cambio);
}
