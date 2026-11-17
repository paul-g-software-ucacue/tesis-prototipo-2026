package ec.mdd.notificaciones.dominio;

import java.util.Objects;
import java.util.Optional;

/**
 * A quién toca un mensaje: los usuarios activos con el rol. RESPONSABLE_CONSOLIDACION es el
 * designado del periodo; con carrera, solo los coordinadores de esa carrera (D-108).
 *
 * @param rol uno de los diez roles de D-63
 * @param carrera la carrera, solo en una devolución
 */
public record DestinoDelMensaje(String rol, Optional<String> carrera) {

    /** Exige los datos. */
    public DestinoDelMensaje {
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(carrera, "carrera");
    }
}
