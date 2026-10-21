package ec.mdd.academico.puertos.salida;

import ec.mdd.academico.puertos.entrada.PeriodoConOtraVersion;
import ec.mdd.academico.puertos.entrada.VersionInexistente;
import java.util.OptionalInt;

/**
 * La versión de normativa de cada periodo, que guarda Normativa (asignación normativa, RF-19).
 * Académico no la copia: se la pide cada vez (research §2). Es un límite entre servicios: si
 * Normativa se separa en su propio proceso, solo cambia el adaptador (research §4, principio V).
 */
public interface NormativaDelPeriodo {

    /** La versión que rige el periodo; vacía si todavía no tiene. */
    OptionalInt versionDe(String codigo);

    /**
     * Asocia la versión al periodo, en una transacción de Normativa. Si el periodo ya tenía esa
     * versión, no cambia nada.
     *
     * @param quien el usuario de la sesión
     * @throws PeriodoConOtraVersion si el periodo ya tiene otra versión
     * @throws VersionInexistente si la versión no existe
     */
    void asociar(String codigo, int version, String quien);
}
