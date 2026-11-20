package ec.mdd.validacionocs.puertos.salida;

import java.util.OptionalInt;

/**
 * Lo que Normativa sabe de un periodo para Aprobación (F12-H03, D-114, ADR-023): los días de RN-23,
 * por la ruta interna del proceso {@code datos}.
 */
@FunctionalInterface
public interface NormativaDelPeriodo {

    /**
     * @param periodo el código del periodo
     * @return los días antes del inicio del periodo en que se remite el distributivo aprobado por
     *     el OCS; vacío si el periodo no tiene versión de normativa
     */
    OptionalInt diasDeRemision(String periodo);
}
