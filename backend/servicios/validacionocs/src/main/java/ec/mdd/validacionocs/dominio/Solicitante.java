package ec.mdd.validacionocs.dominio;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Quien intenta mover el distributivo: su usuario, sus roles y, si solo puede lo propio, su carrera
 * (D-64).
 *
 * @param usuario el nombre de usuario
 * @param roles sus roles, del token
 * @param carreraPropia la carrera a la que se limita; vacía si no tiene alcance propio
 */
public record Solicitante(String usuario, Set<String> roles, Optional<String> carreraPropia) {

    /** Exige los datos y copia los roles. */
    public Solicitante {
        Objects.requireNonNull(usuario, "usuario");
        roles = Set.copyOf(roles);
        Objects.requireNonNull(carreraPropia, "carreraPropia");
    }
}
