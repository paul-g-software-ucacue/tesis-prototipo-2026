package ec.mdd.generacion.dominio;

import java.util.Objects;

/**
 * Un docente de una versión congelada, para elegirlo antes de pedir su contenido (F20-H01).
 *
 * @param identificacion su número de identificación
 * @param nombre sus apellidos y nombres, como en la asignación
 */
public record DocenteDeLaVersion(String identificacion, String nombre) {

    /** Exige los datos. */
    public DocenteDeLaVersion {
        Objects.requireNonNull(identificacion, "identificacion");
        Objects.requireNonNull(nombre, "nombre");
    }
}
