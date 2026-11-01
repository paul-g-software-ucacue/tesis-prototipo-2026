package ec.mdd.generacion.puertos.salida;

/** La bitácora de los cambios del distributivo (principio VII). */
public interface RegistroDeCambios {

    /** Registra un cambio. */
    void registrar(CambioDeDistributivo cambio);
}
