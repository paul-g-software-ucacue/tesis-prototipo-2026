package ec.mdd.normativa.puertos.entrada;

/**
 * Caso de uso: asociar una versión de normativa a un periodo académico (RF-19, F04-H02). Refleja la
 * operación {@code asociarPeriodo} del contrato de Normativa.
 *
 * <p>Es parte de la API publicada del servicio: Académico lo usará al abrir el periodo y elegir su
 * versión (F02-H01), sin leer las tablas de Normativa (principio V). Desde que una versión rige un
 * periodo, ya no cambia. Queda en el registro como {@code VERSION_ASOCIADA}.
 */
public interface AsociarVersionAPeriodo {

    /**
     * @param periodo el código del periodo, por ejemplo {@code 2025-1}; Normativa no comprueba que
     *     exista
     * @param version el número de la versión
     * @param quien el usuario de la sesión
     * @return la asignación del periodo; si ya tenía esa versión, la que tenía, sin cambios
     * @throws VersionNoEncontrada si la versión no existe
     * @throws PeriodoConVersion si el periodo ya tiene otra versión
     */
    AsignacionDelPeriodo asociar(String periodo, int version, String quien);
}
