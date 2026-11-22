package ec.mdd.reporting.dominio;

import java.util.List;
import java.util.Objects;

/**
 * El contenido congelado completo de todos los docentes de una versión, de una sola lectura
 * (F19-H01, D-119).
 *
 * @param hash el hash de la versión
 * @param cargas el contenido de cada docente, por nombre
 */
public record CargasDeLaVersion(String hash, List<ContenidoDelDocente> cargas) {

    /** Exige el hash y copia la lista. */
    public CargasDeLaVersion {
        Objects.requireNonNull(hash, "hash");
        cargas = List.copyOf(cargas);
    }
}
