package ec.mdd.usuarios.puertos.entrada;

/**
 * Lo que la administración de roles ve de un usuario: nunca su contraseña ni su hash (esquema
 * {@code UsuarioResumen}).
 *
 * @param usuario nombre de usuario normalizado
 * @param nombre nombre para mostrar
 * @param activo si puede iniciar sesión
 */
public record ResumenDeUsuario(String usuario, String nombre, boolean activo) {}
