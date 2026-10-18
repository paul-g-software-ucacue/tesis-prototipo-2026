package ec.mdd.usuarios.infraestructura;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Doble escrito a mano del repositorio de usuarios (sin Mockito fuera de la aplicación). */
final class RepositorioEnMemoria implements RepositorioUsuarios {

    final List<Usuario> usuarios = new ArrayList<>();
    int actualizaciones;

    @Override
    public Optional<Usuario> buscarPorNombre(String nombreUsuario) {
        return usuarios.stream()
                .filter(u -> u.nombreUsuario().equals(Usuario.normalizar(nombreUsuario)))
                .findFirst();
    }

    @Override
    public List<Usuario> todos() {
        return usuarios.stream().sorted(Comparator.comparing(Usuario::nombreUsuario)).toList();
    }

    @Override
    public void guardar(Usuario usuario) {
        usuarios.add(usuario);
    }

    @Override
    public void actualizar(Usuario usuario) {
        Usuario anterior = buscarPorNombre(usuario.nombreUsuario()).orElseThrow();
        usuarios.set(usuarios.indexOf(anterior), usuario);
        actualizaciones++;
    }
}
