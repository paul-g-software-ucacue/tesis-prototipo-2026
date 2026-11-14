package ec.mdd.usuarios.dominio;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

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
 * @param identificacion la cédula o el pasaporte de la persona, con el formato de Docentes, si se
 *     conoce (F14-H01, D-102): con ella Firma reconoce al docente de un cargo firmante. Es un dato
 *     personal: no va en el token ni en las respuestas públicas
 */
public record Usuario(
        String nombreUsuario,
        String nombreParaMostrar,
        String claveHash,
        boolean activo,
        Optional<String> identificacion) {

    // El formato de Docentes: cédula de 10 dígitos o pasaporte de hasta 13 letras o dígitos.
    private static final Pattern IDENTIFICACION = Pattern.compile("^[0-9A-Z]{1,13}$");

    /** Un usuario sin identificación. */
    public Usuario(
            String nombreUsuario, String nombreParaMostrar, String claveHash, boolean activo) {
        this(nombreUsuario, nombreParaMostrar, claveHash, activo, Optional.empty());
    }

    /**
     * Exige nombre, nombre para mostrar y hash, y normaliza el nombre. La identificación es
     * opcional; si está, tiene el formato de Docentes (F14-H01, D-102).
     */
    public Usuario {
        Objects.requireNonNull(nombreUsuario, "nombreUsuario");
        Objects.requireNonNull(nombreParaMostrar, "nombreParaMostrar");
        Objects.requireNonNull(claveHash, "claveHash");
        Objects.requireNonNull(identificacion, "identificacion");
        nombreUsuario = normalizar(nombreUsuario);
        if (nombreUsuario.isEmpty() || nombreParaMostrar.isBlank()) {
            throw new IllegalArgumentException("El usuario necesita nombre y nombre para mostrar");
        }
        if (identificacion.filter(una -> !IDENTIFICACION.matcher(una).matches()).isPresent()) {
            throw new IllegalArgumentException("La identificación no tiene el formato de Docentes");
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
