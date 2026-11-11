package ec.mdd.generacion.puertos.entrada;

import java.util.OptionalInt;

/**
 * Si el distributivo del periodo ya está en revisión del Vicerrectorado o después: el consolidado
 * ya no cambia (#215, D-99); y desde F13-H01, si su versión vigente está congelada para la firma
 * (D-100). Lo usan las consultas, para decirlo, y las operaciones del consolidado, para rechazar
 * dentro de su transacción, primero por la versión congelada.
 */
@FunctionalInterface
public interface EnRevision {

    /**
     * @param periodo el código del periodo
     * @return verdadero si el distributivo está en revisión o después; falso sin distributivo
     */
    boolean enRevision(String periodo);

    /**
     * El número de la versión vigente si está congelada (F13-H01, D-100). Dentro de una
     * transacción, la lectura deja un bloqueo compartido de la versión hasta que termina: una
     * congelación que llega después espera.
     *
     * <p>Por defecto, ninguna: así se comportan los dobles de las pruebas que solo miran la etapa;
     * la implementación del proceso la lee de la base.
     *
     * @param periodo el código del periodo
     * @return el número; vacío si no está congelada o no hay distributivo
     */
    default OptionalInt versionCongelada(String periodo) {
        return OptionalInt.empty();
    }
}
