package ec.mdd.roles.puertos.entrada;

import java.util.Objects;
import java.util.Optional;

/**
 * Un usuario a quien toca un mensaje (F16-H01).
 *
 * @param usuario su nombre de usuario
 * @param carrera con COORDINADOR_CARRERA y una carrera pedida, esa carrera; si no, vacío
 */
public record Destinatario(String usuario, Optional<String> carrera) {

    /** Exige los datos. */
    public Destinatario {
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(carrera, "carrera");
    }
}
