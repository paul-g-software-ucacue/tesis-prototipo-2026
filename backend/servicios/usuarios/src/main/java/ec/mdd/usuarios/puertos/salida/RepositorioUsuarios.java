package ec.mdd.usuarios.puertos.salida;

import ec.mdd.usuarios.dominio.Usuario;
import java.util.Optional;

/** Persistencia de los usuarios, en el esquema {@code usuarios}. */
public interface RepositorioUsuarios {

    /** Busca un usuario por su nombre normalizado ({@link Usuario#normalizar(String)}). */
    Optional<Usuario> buscarPorNombre(String nombreUsuario);

    /** Guarda un usuario nuevo. El nombre no puede repetirse. */
    void guardar(Usuario usuario);

    /**
     * Reemplaza el nombre para mostrar, el hash, si está activo y los roles de un usuario que ya
     * existe, buscado por su nombre.
     *
     * @throws IllegalStateException si el usuario no existe
     */
    void actualizar(Usuario usuario);
}
