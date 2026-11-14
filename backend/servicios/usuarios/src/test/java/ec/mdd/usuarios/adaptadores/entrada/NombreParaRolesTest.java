package ec.mdd.usuarios.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** El nombre para mostrar que Roles pide para el bloque de firmas (F13-H02, patrón de D-99). */
class NombreParaRolesTest {

    private final RepositorioUsuarios usuarios = mock(RepositorioUsuarios.class);
    private final NombreParaRoles nombres = new NombreParaRoles(usuarios);

    @Test
    void dadoUnUsuario_cuandoSePideSuNombre_entoncesElNombreParaMostrar() {
        when(usuarios.buscarPorNombre("consolidacion.demo"))
                .thenReturn(
                        Optional.of(
                                new Usuario("consolidacion.demo", "PÉREZ ANA", "{noop}x", true)));

        assertThat(nombres.nombre("consolidacion.demo")).contains("PÉREZ ANA");
    }

    @Test
    void dadoUnUsuarioQueNoExiste_cuandoSePideSuNombre_entoncesVacio() {
        when(usuarios.buscarPorNombre("nadie")).thenReturn(Optional.empty());

        assertThat(nombres.nombre("nadie")).isEmpty();
    }
}
