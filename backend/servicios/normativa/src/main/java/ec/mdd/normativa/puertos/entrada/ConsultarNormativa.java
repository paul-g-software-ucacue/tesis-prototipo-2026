package ec.mdd.normativa.puertos.entrada;

import ec.mdd.normativa.dominio.VersionNormativa;
import java.util.List;

/**
 * Caso de uso: consultar las versiones de normativa con su tabla de carga horaria y sus reglas
 * (RF-19, F04-H01), y la versión que rige un periodo (F04-H02). Refleja las operaciones {@code
 * listarVersiones}, {@code consultarVersion} y {@code consultarPeriodo} del contrato de Normativa.
 */
public interface ConsultarNormativa {

    /** Todas las versiones, por número. */
    List<VersionNormativa> versiones();

    /**
     * @throws VersionNoEncontrada si no hay una versión con ese número
     */
    VersionNormativa version(int numero);

    /**
     * @throws PeriodoSinVersion si el periodo no tiene una versión asociada
     */
    AsignacionDelPeriodo periodo(String periodo);
}
