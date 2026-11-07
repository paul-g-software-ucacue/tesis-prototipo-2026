package ec.mdd.usuarios.puertos.salida;

import ec.mdd.usuarios.dominio.EntradaBitacora;

/**
 * La bitácora de auditoría (F17-H01, RF-44). Solo admite agregar: ninguna implementación modifica
 * ni borra una entrada (FR-009, FR-010).
 */
public interface RepositorioBitacora {

    /**
     * Guarda la entrada en la transacción en curso.
     *
     * @param entrada una entrada válida, con un {@code idEvento} que no estaba registrado
     */
    void registrar(EntradaBitacora entrada);
}
