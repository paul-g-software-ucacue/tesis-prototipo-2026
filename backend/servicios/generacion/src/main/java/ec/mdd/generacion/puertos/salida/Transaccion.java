package ec.mdd.generacion.puertos.salida;

import java.util.function.Supplier;

/** Una transacción de la base: lo que pasa dentro se guarda entero o no se guarda. */
public interface Transaccion {

    /** Ejecuta el trabajo en una transacción y devuelve su resultado. */
    <T> T ejecutar(Supplier<T> trabajo);
}
