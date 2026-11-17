package ec.mdd.validacionocs.dominio;

import java.util.Objects;
import java.util.Optional;

/**
 * A quién toca un mensaje de una transición (F16-H01, D-108): los usuarios del rol, que
 * Notificaciones resuelve en el proceso {@code identidad}. RESPONSABLE_CONSOLIDACION es el
 * designado del periodo.
 *
 * @param rol el rol responsable de la etapa, de la copia de la configuración
 * @param carrera solo en una devolución: la carrera devuelta, para sus coordinadores
 */
public record DestinoDeNotificacion(String rol, Optional<String> carrera) {

    /** Exige los datos. */
    public DestinoDeNotificacion {
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(carrera, "carrera");
    }
}
