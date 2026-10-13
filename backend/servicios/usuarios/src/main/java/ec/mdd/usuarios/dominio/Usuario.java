package ec.mdd.usuarios.dominio;

import java.util.Objects;
import java.util.Set;

/**
 * Usuario del MDD que puede iniciar sesión.
 *
 * <p>Guarda el hash de la contraseña, nunca la contraseña. Los roles son los identificadores que
 * viajan en el token; qué permite cada uno es de F05-H02 (D-16).
 *
 * @param nombreUsuario nombre con el que inicia sesión; es el {@code sub} del token
 * @param claveHash hash Argon2id de la contraseña
 * @param activo si puede iniciar sesión
 * @param roles roles del usuario; puede estar vacío
 */
public record Usuario(String nombreUsuario, String claveHash, boolean activo, Set<String> roles) {

    /** Exige nombre y hash, y copia los roles para que el usuario sea inmutable. */
    public Usuario {
        Objects.requireNonNull(nombreUsuario, "nombreUsuario");
        Objects.requireNonNull(claveHash, "claveHash");
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    /** Solo un usuario activo puede iniciar sesión. */
    public boolean puedeIniciarSesion() {
        return activo;
    }

    /** No incluye el hash de la contraseña, para que no termine en un registro. */
    @Override
    public String toString() {
        return "Usuario[nombreUsuario="
                + nombreUsuario
                + ", activo="
                + activo
                + ", roles="
                + roles
                + "]";
    }
}
