package ec.mdd.usuarios.puertos.salida;

import ec.mdd.usuarios.dominio.Usuario;
import java.util.Optional;

/** Persistencia de los usuarios, en el esquema {@code usuarios}. */
public interface RepositorioUsuarios {

    /** Busca un usuario por su nombre exacto. */
    Optional<Usuario> buscarPorNombre(String nombreUsuario);

    /** Guarda un usuario nuevo. El nombre no puede repetirse. */
    void guardar(Usuario usuario);
}
