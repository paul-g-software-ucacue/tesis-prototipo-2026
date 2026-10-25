package ec.mdd.docentes.puertos.salida;

import java.util.function.Supplier;

/**
 * Una transacción de la base: lo que pasa dentro se guarda entero o no se guarda. La aplicación la
 * usa para que la comprobación, el cambio y su registro vayan juntos. Las referencias de Académico
 * se piden antes, fuera de ella: cada servicio tiene su transacción (principio V).
 */
public interface Transaccion {

    /** Ejecuta el trabajo en una transacción y devuelve su resultado. */
    <T> T ejecutar(Supplier<T> trabajo);
}
