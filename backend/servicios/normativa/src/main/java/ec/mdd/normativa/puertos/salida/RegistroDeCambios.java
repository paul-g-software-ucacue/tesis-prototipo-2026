package ec.mdd.normativa.puertos.salida;

/** Guarda cada cambio de la normativa, para la bitácora (FR-009). */
public interface RegistroDeCambios {

    /**
     * Guarda el cambio y lo envía a la bitácora de Usuarios (F17-H01). Se llama dentro de la
     * transacción del cambio: si se revierte, el registro también; sin transacción, la entrada de
     * la bitácora no se publica.
     *
     * @param cambio el cambio ya validado; nunca con secretos
     */
    void registrar(CambioDeNormativa cambio);
}
