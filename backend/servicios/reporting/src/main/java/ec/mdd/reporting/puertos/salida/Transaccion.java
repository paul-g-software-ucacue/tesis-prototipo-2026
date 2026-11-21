package ec.mdd.reporting.puertos.salida;

import java.util.function.Supplier;

/** Una transacción de la base del proceso, para el registro de una descarga (F20-H01). */
public interface Transaccion {

    /**
     * @param trabajo lo que se hace dentro de la transacción
     * @return su resultado
     */
    <T> T ejecutar(Supplier<T> trabajo);
}
