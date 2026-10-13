package ec.mdd.usuarios.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.dominio.Usuario;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Conversión entre la fila y el usuario del dominio, sin base de datos ni repositorio simulado. */
class UsuarioJpaTest {

    @Test
    void dadoUnUsuario_cuandoSeConvierteEnFilaYDeVuelta_entoncesConservaSusDatos() {
        Usuario usuario = new Usuario("autor.prueba", "hash", true, Set.of("DOCENTE"));

        assertThat(UsuarioJpa.nuevo(usuario).aDominio()).isEqualTo(usuario);
    }
}
