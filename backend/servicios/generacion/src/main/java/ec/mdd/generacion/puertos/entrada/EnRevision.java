package ec.mdd.generacion.puertos.entrada;

/**
 * Si el distributivo del periodo ya está en revisión del Vicerrectorado o después: el consolidado
 * ya no cambia (#215, D-99). Lo usan las consultas, para decirlo, y las operaciones del
 * consolidado, para rechazar dentro de su transacción.
 */
@FunctionalInterface
public interface EnRevision {

    /**
     * @param periodo el código del periodo
     * @return verdadero si el distributivo está en revisión o después; falso sin distributivo
     */
    boolean enRevision(String periodo);
}
