package ec.mdd.validacionocs.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Un archivo cargado para un periodo.
 *
 * @param numero su versión en el periodo, desde 1
 * @param contenido el archivo tal como se subió
 * @param por quién lo cargó
 * @param en cuándo
 * @param advertencia la advertencia de RN-23 con la que se cargó, si hubo (F12-H03, FR-014)
 */
public record CargaGuardada(
        int numero,
        String contenido,
        String por,
        Instant en,
        Optional<AdvertenciaDeRemision> advertencia) {

    /** Exige la advertencia, presente o vacía. */
    public CargaGuardada {
        Objects.requireNonNull(advertencia, "advertencia");
    }

    /** Una carga sin advertencia. */
    public CargaGuardada(int numero, String contenido, String por, Instant en) {
        this(numero, contenido, por, en, Optional.empty());
    }
}
