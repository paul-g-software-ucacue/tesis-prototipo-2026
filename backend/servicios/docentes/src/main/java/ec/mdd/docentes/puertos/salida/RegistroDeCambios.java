package ec.mdd.docentes.puertos.salida;

/** Guarda cada cambio de los docentes, para la bitácora (FR-010, principio VII). */
public interface RegistroDeCambios {

    /** Guarda el cambio en la transacción en curso. */
    void registrar(CambioRegistrado cambio);
}
