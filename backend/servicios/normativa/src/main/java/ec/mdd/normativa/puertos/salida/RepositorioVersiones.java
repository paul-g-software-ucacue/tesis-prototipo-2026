package ec.mdd.normativa.puertos.salida;

import ec.mdd.normativa.dominio.VersionNormativa;
import java.util.List;
import java.util.Optional;

/** Las versiones de normativa del esquema {@code normativa}, reconstruidas con el agregado. */
public interface RepositorioVersiones {

    /** Todas las versiones, por número. */
    List<VersionNormativa> todas();

    /** La versión con ese número, si existe. */
    Optional<VersionNormativa> buscar(int numero);
}
