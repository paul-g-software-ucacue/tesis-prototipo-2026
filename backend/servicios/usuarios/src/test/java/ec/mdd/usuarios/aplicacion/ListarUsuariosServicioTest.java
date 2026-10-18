package ec.mdd.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.ResumenDeUsuario;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import java.util.List;
import org.junit.jupiter.api.Test;

/** La lista de usuarios para asignarles roles, sin hashes (research §10), con Mockito. */
class ListarUsuariosServicioTest {

    private final RepositorioUsuarios repositorio = mock(RepositorioUsuarios.class);

    @Test
    void dadoUsuarios_cuandoSeListan_entoncesSoloSuNombreSuNombreParaMostrarYSiEstaActivo() {
        when(repositorio.todos())
                .thenReturn(
                        List.of(
                                new Usuario("admin.demo", "Administración (demo)", "hash-a", true),
                                new Usuario("autor.prueba", "Autor de prueba", "hash-b", false)));

        assertThat(new ListarUsuariosServicio(repositorio).listar())
                .containsExactly(
                        new ResumenDeUsuario("admin.demo", "Administración (demo)", true),
                        new ResumenDeUsuario("autor.prueba", "Autor de prueba", false));
    }
}
