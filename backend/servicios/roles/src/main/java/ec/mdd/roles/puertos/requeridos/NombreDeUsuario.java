package ec.mdd.roles.puertos.requeridos;

import java.util.Optional;

/**
 * El nombre para mostrar de un usuario (D-59), que es de Usuarios (F13-H02). Lo implementa Usuarios
 * en el proceso {@code identidad}; Roles lo usa para nombrar al Responsable de consolidación en el
 * bloque de firmas del documento firmable.
 */
@FunctionalInterface
public interface NombreDeUsuario {

    /**
     * @param usuario el nombre de usuario
     * @return su nombre para mostrar; vacío si no tiene o el usuario no existe
     */
    Optional<String> nombre(String usuario);
}
