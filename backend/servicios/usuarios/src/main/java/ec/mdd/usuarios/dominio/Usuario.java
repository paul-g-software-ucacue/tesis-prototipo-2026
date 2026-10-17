package ec.mdd.usuarios.dominio;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Usuario del MDD que puede iniciar sesión.
 *
 * <p>Guarda el hash de la contraseña, nunca la contraseña. Los roles son los identificadores que
 * viajan en el token; qué permite cada uno es de F05-H02 (D-16). El nombre de usuario no distingue
 * mayúsculas y minúsculas: se guarda normalizado.
 *
 * @param nombreUsuario nombre con el que inicia sesión, normalizado; es el {@code sub} del token
 * @param nombreParaMostrar nombre que ve el usuario en la pantalla de inicio
 * @param claveHash hash Argon2id de la contraseña
 * @param activo si puede iniciar sesión
 * @param roles roles del usuario; puede estar vacío
 */
public record Usuario(
        String nombreUsuario,
        String nombreParaMostrar,
        String claveHash,
        boolean activo,
        Set<String> roles) {

    /** Exige nombre, nombre para mostrar y hash, normaliza el nombre y copia los roles. */
    public Usuario {
        Objects.requireNonNull(nombreUsuario, "nombreUsuario");
        Objects.requireNonNull(nombreParaMostrar, "nombreParaMostrar");
        Objects.requireNonNull(claveHash, "claveHash");
        nombreUsuario = normalizar(nombreUsuario);
        if (nombreUsuario.isEmpty() || nombreParaMostrar.isBlank()) {
            throw new IllegalArgumentException("El usuario necesita nombre y nombre para mostrar");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    /**
     * El nombre de usuario como se guarda y se busca: sin espacios alrededor y en minúsculas.
     *
     * @param nombreUsuario nombre tal como se escribió; puede ser {@code null}
     * @return el nombre normalizado; vacío si no había nombre
     */
    public static String normalizar(String nombreUsuario) {
        return nombreUsuario == null ? "" : nombreUsuario.strip().toLowerCase(Locale.ROOT);
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
