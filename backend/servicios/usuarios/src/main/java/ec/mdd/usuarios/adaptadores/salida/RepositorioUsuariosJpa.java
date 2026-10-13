package ec.mdd.usuarios.adaptadores.salida;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import java.util.Optional;

/** {@link RepositorioUsuarios} sobre PostgreSQL con Spring Data JPA (constitución §3). */
public final class RepositorioUsuariosJpa implements RepositorioUsuarios {

    private final UsuarioJpaRepositorio filas;

    /**
     * @param filas repositorio de Spring Data
     */
    public RepositorioUsuariosJpa(UsuarioJpaRepositorio filas) {
        this.filas = filas;
    }

    @Override
    public Optional<Usuario> buscarPorNombre(String nombreUsuario) {
        return filas.findByNombreUsuario(nombreUsuario).map(UsuarioJpa::aDominio);
    }

    @Override
    public void guardar(Usuario usuario) {
        filas.save(UsuarioJpa.nuevo(usuario));
    }
}
