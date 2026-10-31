package ec.mdd.roles.puertos.salida;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** La carrera de cada coordinador, en el esquema {@code roles} (F06-H05). */
public interface RepositorioCarrerasDeCoordinadores {

    /** La carrera de cada usuario que tiene una. */
    Map<String, String> todas();

    /** La carrera del usuario, si tiene. */
    Optional<String> de(String usuario);

    /** Guarda la carrera del usuario, con quién y cuándo; reemplaza la que tenga. */
    void guardar(String usuario, String carrera, String quien, Instant momento);

    /**
     * Quita la carrera del usuario.
     *
     * @return si tenía una
     */
    boolean quitar(String usuario);
}
