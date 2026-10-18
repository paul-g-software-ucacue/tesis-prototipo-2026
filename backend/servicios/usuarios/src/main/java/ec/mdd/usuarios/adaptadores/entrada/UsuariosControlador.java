package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.usuarios.adaptadores.entrada.api.UsuarioResumen;
import ec.mdd.usuarios.adaptadores.entrada.api.UsuariosApi;
import ec.mdd.usuarios.puertos.entrada.ListarUsuarios;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * La lista de usuarios para la administración de roles ({@code contratos/usuarios/openapi.yaml},
 * F05-H02). Nunca devuelve contraseñas ni hashes. Exige «Usuarios y roles: lectura», como dice su
 * {@code x-mdd-permiso} (ADR-021).
 */
@RestController
class UsuariosControlador implements UsuariosApi {

    private final ListarUsuarios listar;

    UsuariosControlador(ListarUsuarios listar) {
        this.listar = listar;
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_USUARIOS_ROLES:LECTURA')")
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
