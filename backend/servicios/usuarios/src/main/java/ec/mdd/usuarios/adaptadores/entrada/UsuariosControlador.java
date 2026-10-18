package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.usuarios.adaptadores.entrada.api.UsuarioResumen;
import ec.mdd.usuarios.adaptadores.entrada.api.UsuariosApi;
import ec.mdd.usuarios.puertos.entrada.ListarUsuarios;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * La lista de usuarios para la administración de roles ({@code contratos/usuarios/openapi.yaml},
 * F05-H02). Nunca devuelve contraseñas ni hashes.
 */
@RestController
class UsuariosControlador implements UsuariosApi {

    private final ListarUsuarios listar;

    UsuariosControlador(ListarUsuarios listar) {
        this.listar = listar;
    }

    @Override
    public ResponseEntity<List<UsuarioResumen>> listarUsuarios() {
        return ResponseEntity.ok(
                listar.listar().stream()
                        .map(
                                usuario ->
                                        new UsuarioResumen(
                                                usuario.usuario(),
                                                usuario.nombre(),
                                                usuario.activo()))
                        .toList());
    }
}
