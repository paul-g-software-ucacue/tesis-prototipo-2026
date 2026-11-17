package ec.mdd.notificaciones.dominio;

import java.util.Objects;
import java.util.Optional;

/**
 * Un usuario activo a quien toca un mensaje, ya resuelto en el proceso {@code identidad} (F16-H01).
 *
 * @param usuario su nombre de usuario
 * @param carrera en una devolución, la carrera de su notificación
 */
public record Destinatario(String usuario, Optional<String> carrera) {

    /** Exige los datos. */
    public Destinatario {
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(carrera, "carrera");
    }
}
