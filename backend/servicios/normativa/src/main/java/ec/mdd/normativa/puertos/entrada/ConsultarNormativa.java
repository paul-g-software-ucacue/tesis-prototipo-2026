package ec.mdd.normativa.puertos.entrada;

import ec.mdd.normativa.dominio.VersionNormativa;
import java.util.List;

/**
 * Caso de uso: consultar las versiones de normativa con su tabla de carga horaria y sus reglas
 * (RF-19, F04-H01). Refleja las operaciones {@code listarVersiones} y {@code consultarVersion} del
 * contrato de Normativa.
 */
public interface ConsultarNormativa {

    /** Todas las versiones, por número. */
    List<VersionNormativa> versiones();

    /**
     * @throws VersionNoEncontrada si no hay una versión con ese número
     */
    VersionNormativa version(int numero);
}
