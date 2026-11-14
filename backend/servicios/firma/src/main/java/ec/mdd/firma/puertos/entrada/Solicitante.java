package ec.mdd.firma.puertos.entrada;

import java.util.Objects;
import java.util.Set;

/**
 * Quién pide una operación de Firma: su usuario y sus roles, del token (F13-H02).
 *
 * @param usuario el nombre de usuario
 * @param roles sus roles
 */
public record Solicitante(String usuario, Set<String> roles) {

    /** Exige los datos y copia los roles. */
    public Solicitante {
        Objects.requireNonNull(usuario, "usuario");
        roles = Set.copyOf(roles);
    }
}
