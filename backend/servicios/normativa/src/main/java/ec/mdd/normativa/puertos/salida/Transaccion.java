package ec.mdd.normativa.puertos.salida;

import java.util.function.Supplier;

/**
 * Una transacción de la base: lo que pasa dentro se guarda entero o no se guarda (SC-004). La
 * aplicación la usa para que la comprobación, el cambio y su registro vayan juntos.
 */
public interface Transaccion {

    /** Ejecuta el trabajo en una transacción y devuelve su resultado. */
    <T> T ejecutar(Supplier<T> trabajo);
}
