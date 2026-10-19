package ec.mdd.normativa.puertos.entrada;

import ec.mdd.normativa.dominio.CambiosDeVersion;
import ec.mdd.normativa.dominio.VersionNormativa;

/**
 * Caso de uso: editar una versión de normativa que no rige ningún periodo (RF-19, F04-H02). Refleja
 * la operación {@code editarVersion} del contrato de Normativa.
 *
 * <p>Es la única forma de cambiar una versión (FR-010): la pantalla «Normativa» la usa, y la carga
 * 7 por archivo de F01 la usará con la tabla completa. Así las dos pasan por las mismas reglas.
 * Cada edición que cambia algo queda en el registro como {@code VERSION_EDITADA}, con cada valor
 * anterior y nuevo.
 */
public interface EditarVersion {

    /**
     * @param numero el número de la versión
     * @param cambios la edición completa: la cabecera, todas las filas y todos los parámetros
     * @param quien el usuario de la sesión
     * @return la versión editada; la misma si nada cambió
     * @throws VersionNoEncontrada si la versión no existe
     * @throws ec.mdd.normativa.dominio.VersionEnUso si la versión rige un periodo
     * @throws ec.mdd.normativa.dominio.CambioNoPermitido si se agrega o se quita una fila, una
     *     regla o un parámetro, o un parámetro no es válido
     * @throws ec.mdd.normativa.dominio.FilaInvalida si una fila no cumple sus invariantes
     * @throws CodigoRepetido si otra versión ya usa el código nuevo
     */
    VersionNormativa editar(int numero, CambiosDeVersion cambios, String quien);
}
