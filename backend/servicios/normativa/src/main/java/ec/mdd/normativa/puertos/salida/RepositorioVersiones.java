package ec.mdd.normativa.puertos.salida;

import ec.mdd.normativa.dominio.VersionNormativa;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Las versiones de normativa del esquema {@code normativa}, reconstruidas con el agregado.
 *
 * <p>{@link #siguienteNumero}, {@link #bloquear} y {@link #bloquearPeriodo} solo tienen sentido
 * dentro de una {@link Transaccion}: sus candados duran hasta que termina (research §3).
 */
public interface RepositorioVersiones {

    /** Todas las versiones, por número. */
    List<VersionNormativa> todas();

    /** La versión con ese número, si existe. */
    Optional<VersionNormativa> buscar(int numero);

    /**
     * La versión con ese número, bloqueada hasta que termine la transacción: otra edición o
     * asociación de la misma versión espera (research §3).
     */
    Optional<VersionNormativa> bloquear(int numero);

    /**
     * El número de la versión nueva, el siguiente al más alto. Toma un candado de la transacción,
     * así que dos creaciones simultáneas obtienen números distintos.
     */
    int siguienteNumero();

    /** Si otra versión, distinta de {@code salvo}, ya usa ese código. */
    boolean existeCodigo(String codigo, int salvo);

    /** Toma un candado de la transacción sobre el periodo: dos asociaciones al mismo esperan. */
    void bloquearPeriodo(String periodo);

    /** La versión que rige el periodo, si tiene. */
    Optional<VersionNormativa> porPeriodo(String periodo);

    /**
     * Guarda la versión: la crea si no existe, o reemplaza su cabecera, sus filas y sus parámetros,
     * y agrega sus asignaciones nuevas. Todo en la transacción en curso.
     *
     * @param version la versión
     * @param quien el usuario de la sesión, para las asignaciones nuevas
     * @param momento cuándo, para la creación
     */
    void guardar(VersionNormativa version, String quien, Instant momento);
}
