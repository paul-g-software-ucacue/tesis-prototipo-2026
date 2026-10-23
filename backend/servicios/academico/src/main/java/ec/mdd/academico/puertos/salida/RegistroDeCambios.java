package ec.mdd.academico.puertos.salida;

/** Guarda cada cambio de los periodos y de la estructura orgánica, para la bitácora. */
public interface RegistroDeCambios {

    /** Guarda el cambio en la transacción en curso. */
    void registrar(CambioRegistrado cambio);
}
