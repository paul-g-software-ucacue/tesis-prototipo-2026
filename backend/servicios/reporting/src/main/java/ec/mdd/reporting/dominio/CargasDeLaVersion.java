package ec.mdd.reporting.dominio;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * El contenido congelado completo de todos los docentes de una versión, de una sola lectura
 * (F19-H01, D-119).
 *
 * @param hash el hash de la versión
 * @param cargas el contenido de cada docente, por nombre
 * @param inconsistencias las abiertas que Generación registró al congelar la versión; vacío si no
 *     las registró (F19-H02, D-120)
 */
public record CargasDeLaVersion(
        String hash,
        List<ContenidoDelDocente> cargas,
        Optional<InconsistenciasAbiertas> inconsistencias) {

    /** Un contenido sin inconsistencias registradas. */
    public CargasDeLaVersion(String hash, List<ContenidoDelDocente> cargas) {
        this(hash, cargas, Optional.empty());
    }

    /** Exige el hash y copia la lista. */
    public CargasDeLaVersion {
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(inconsistencias, "inconsistencias");
        cargas = List.copyOf(cargas);
    }
}
