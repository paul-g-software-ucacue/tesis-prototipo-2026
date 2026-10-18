package ec.mdd.usuarios.dominio;

import java.util.Locale;
import java.util.Objects;

/**
 * Usuario del MDD que puede iniciar sesión.
 *
 * <p>Guarda el hash de la contraseña, nunca la contraseña. Sus roles y permisos no están aquí: son
 * de Roles, que los da al iniciar sesión y al renovar el token (ADR-021). El nombre de usuario no
 * distingue mayúsculas y minúsculas: se guarda normalizado.
 *
 * @param nombreUsuario nombre con el que inicia sesión, normalizado; es el {@code sub} del token
 * @param nombreParaMostrar nombre que ve el usuario en la pantalla de inicio
 * @param claveHash hash Argon2id de la contraseña
 * @param activo si puede iniciar sesión
 */
public record Usuario(
        String nombreUsuario, String nombreParaMostrar, String claveHash, boolean activo) {

    /** Exige nombre, nombre para mostrar y hash, y normaliza el nombre. */
    public Usuario {
        Objects.requireNonNull(nombreUsuario, "nombreUsuario");
        Objects.requireNonNull(nombreParaMostrar, "nombreParaMostrar");
        Objects.requireNonNull(claveHash, "claveHash");
        nombreUsuario = normalizar(nombreUsuario);
        if (nombreUsuario.isEmpty() || nombreParaMostrar.isBlank()) {
            throw new IllegalArgumentException("El usuario necesita nombre y nombre para mostrar");
        }
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
        return "Usuario[nombreUsuario=" + nombreUsuario + ", activo=" + activo + "]";
    }
}
