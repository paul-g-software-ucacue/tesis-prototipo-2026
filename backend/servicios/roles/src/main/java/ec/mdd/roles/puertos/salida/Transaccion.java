package ec.mdd.roles.puertos.salida;

import java.util.function.Supplier;

/**
 * Una transacción de la base: lo que pasa dentro se guarda entero o no se guarda. La designación la
 * usa para que el candado del periodo, el cambio y su registro vayan juntos, y el receptor de
 * eventos para que la marca del evento y la copia del periodo vayan juntas (F05-H03).
 */
public interface Transaccion {

    /** Ejecuta el trabajo en una transacción y devuelve su resultado. */
    <T> T ejecutar(Supplier<T> trabajo);
}
