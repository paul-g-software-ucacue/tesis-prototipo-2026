package ec.mdd.validacionocs.puertos.salida;

import java.util.function.Supplier;

/**
 * Una transacción de la base: lo que pasa dentro se guarda entero o no se guarda. La carga de la
 * configuración la usa para que la fila y su entrada de bitácora vayan juntas (F17-H01).
 */
public interface Transaccion {

    /** Ejecuta el trabajo en una transacción y devuelve su resultado. */
    <T> T ejecutar(Supplier<T> trabajo);
}
