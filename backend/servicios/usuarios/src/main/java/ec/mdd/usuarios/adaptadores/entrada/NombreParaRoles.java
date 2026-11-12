package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.roles.puertos.requeridos.NombreDeUsuario;
import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import java.util.Optional;

/**
 * El nombre para mostrar de un usuario (D-59), para el puerto que Roles requiere (F13-H02, patrón
 * de D-99): Usuarios ya depende de Roles, así que es Usuarios quien lo implementa. Solo lee.
 */
public final class NombreParaRoles implements NombreDeUsuario {

    private final RepositorioUsuarios usuarios;

    /** Recibe los usuarios. */
    public NombreParaRoles(RepositorioUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<String> nombre(String usuario) {
        return usuarios.buscarPorNombre(usuario).map(Usuario::nombreParaMostrar);
    }
}
