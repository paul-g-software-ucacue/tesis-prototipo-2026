package ec.mdd.roles.dominio;

import java.util.Locale;

/**
 * El nombre de usuario como lo guarda Roles: sin espacios a los lados y en minúsculas, igual que
 * Usuarios (F05-H01). Roles no comprueba que el usuario exista (research §2).
 */
public final class NombreDeUsuario {

    private NombreDeUsuario() {}

    /** El nombre normalizado; vacío si viene {@code null}. */
    public static String normalizar(String nombre) {
        return nombre == null ? "" : nombre.strip().toLowerCase(Locale.ROOT);
    }
}
