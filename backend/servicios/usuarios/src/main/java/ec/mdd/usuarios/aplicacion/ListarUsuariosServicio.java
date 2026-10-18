package ec.mdd.usuarios.aplicacion;

import ec.mdd.usuarios.puertos.entrada.ListarUsuarios;
import ec.mdd.usuarios.puertos.entrada.ResumenDeUsuario;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import java.util.List;

/** La lista de usuarios sin datos sensibles. */
public final class ListarUsuariosServicio implements ListarUsuarios {

    private final RepositorioUsuarios repositorio;

    /** Recibe el repositorio de usuarios. */
    public ListarUsuariosServicio(RepositorioUsuarios repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<ResumenDeUsuario> listar() {
        return repositorio.todos().stream()
                .map(
                        usuario ->
                                new ResumenDeUsuario(
                                        usuario.nombreUsuario(),
                                        usuario.nombreParaMostrar(),
                                        usuario.activo()))
                .toList();
    }
}
