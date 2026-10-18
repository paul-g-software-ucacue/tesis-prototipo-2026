package ec.mdd.usuarios.puertos.entrada;

import java.util.List;

/**
 * Caso de uso: la lista de usuarios, para que el administrador les asigne roles (F05-H02, research
 * §10). Crear o desactivar usuarios no es de esta historia.
 */
public interface ListarUsuarios {

    /** Todos los usuarios, ordenados por nombre de usuario. */
    List<ResumenDeUsuario> listar();
}
