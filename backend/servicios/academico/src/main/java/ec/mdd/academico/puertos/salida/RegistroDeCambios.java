package ec.mdd.academico.puertos.salida;

/** Guarda cada cambio de los periodos, para la bitácora (FR-008). */
public interface RegistroDeCambios {

    /** Guarda el cambio en la transacción en curso. */
    void registrar(CambioDePeriodo cambio);
}
