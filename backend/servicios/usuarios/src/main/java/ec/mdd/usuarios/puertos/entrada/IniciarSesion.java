package ec.mdd.usuarios.puertos.entrada;

/**
 * Caso de uso: iniciar sesión con usuario y contraseña (RF-01, D-37).
 *
 * <p>Si las credenciales son correctas y el usuario está activo, devuelve un token firmado. Si no,
 * lanza {@code CredencialesInvalidas}, sin decir cuál de los datos falló.
 */
public interface IniciarSesion {

    /**
     * Inicia sesión.
     *
     * @param usuario nombre de usuario; puede venir vacío
     * @param clave contraseña; puede venir vacía
     * @return el token emitido y su vencimiento
     */
    TokenEmitido iniciar(String usuario, String clave);
}
