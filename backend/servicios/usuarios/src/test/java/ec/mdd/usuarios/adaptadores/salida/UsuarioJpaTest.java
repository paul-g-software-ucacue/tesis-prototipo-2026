package ec.mdd.usuarios.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.dominio.Usuario;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Conversión entre la fila y el usuario del dominio, sin base de datos ni repositorio simulado. */
class UsuarioJpaTest {

    @Test
    void dadoUnUsuario_cuandoSeConvierteEnFilaYDeVuelta_entoncesConservaSusDatos() {
        Usuario usuario = new Usuario("autor.prueba", "Autor", "hash", true, Set.of("DOCENTE"));

        assertThat(UsuarioJpa.nuevo(usuario).aDominio()).isEqualTo(usuario);
    }

    @Test
    void dadoUnaFila_cuandoCopiaOtroUsuario_entoncesConservaElNombreYCambiaLoDemas() {
        UsuarioJpa fila =
                UsuarioJpa.nuevo(new Usuario("docente.demo", "Viejo", "hash", false, Set.of()));
        Usuario nuevo = new Usuario("docente.demo", "Docente", "otro", true, Set.of("DOCENTE"));

        fila.copiar(nuevo);

        assertThat(fila.aDominio()).isEqualTo(nuevo);
    }
}
