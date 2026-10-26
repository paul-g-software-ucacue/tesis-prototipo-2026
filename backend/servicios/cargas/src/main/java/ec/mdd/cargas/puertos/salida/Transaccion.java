package ec.mdd.cargas.puertos.salida;

import java.util.function.Supplier;

/** Una transacción de la base de Cargas, para el historial. */
public interface Transaccion {

    /** Ejecuta el trabajo en una transacción; una excepción la deshace. */
    <T> T ejecutar(Supplier<T> trabajo);
}
