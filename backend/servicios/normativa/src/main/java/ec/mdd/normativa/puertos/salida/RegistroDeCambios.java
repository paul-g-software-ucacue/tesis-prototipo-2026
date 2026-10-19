package ec.mdd.normativa.puertos.salida;

/** Guarda cada cambio de la normativa, para la bitácora (FR-009). */
public interface RegistroDeCambios {

    /** Guarda el cambio en la transacción en curso. */
    void registrar(CambioDeNormativa cambio);
}
