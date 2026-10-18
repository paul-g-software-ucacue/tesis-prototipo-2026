package ec.mdd.usuarios.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.dominio.Usuario;
import org.junit.jupiter.api.Test;

/** Conversión entre la fila y el usuario del dominio, sin base de datos ni repositorio simulado. */
class UsuarioJpaTest {

    @Test
    void dadoUnUsuario_cuandoSeConvierteEnFilaYDeVuelta_entoncesConservaSusDatos() {
        Usuario usuario = new Usuario("autor.prueba", "Autor", "hash", true);

        assertThat(UsuarioJpa.nuevo(usuario).aDominio()).isEqualTo(usuario);
    }

    @Test
    void dadoUnaFila_cuandoCopiaOtroUsuario_entoncesConservaElNombreYCambiaLoDemas() {
        UsuarioJpa fila = UsuarioJpa.nuevo(new Usuario("docente.demo", "Viejo", "hash", false));
        Usuario nuevo = new Usuario("docente.demo", "Docente", "otro", true);

        fila.copiar(nuevo);

        assertThat(fila.aDominio()).isEqualTo(nuevo);
    }
}
