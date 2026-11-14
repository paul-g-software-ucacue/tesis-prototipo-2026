package ec.mdd.roles.puertos.entrada;

import java.util.Objects;

/**
 * El Responsable de consolidación designado en un periodo (ADR-003, F13-H02).
 *
 * @param usuario su nombre de usuario
 * @param nombre su nombre para mostrar; si no tiene, el nombre de usuario
 */
public record ResponsableDelPeriodo(String usuario, String nombre) {

    /** Exige los datos. */
    public ResponsableDelPeriodo {
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(nombre, "nombre");
    }
}
