package ec.mdd.academico.puertos.salida;

import java.util.function.Supplier;

/**
 * Una transacción de la base: lo que pasa dentro se guarda entero o no se guarda. La aplicación la
 * usa para que la comprobación, el cambio y su registro vayan juntos. Nunca envuelve una llamada a
 * Normativa: cada servicio tiene su transacción (principio V, research §3).
 */
public interface Transaccion {

    /** Ejecuta el trabajo en una transacción y devuelve su resultado. */
    <T> T ejecutar(Supplier<T> trabajo);
}
