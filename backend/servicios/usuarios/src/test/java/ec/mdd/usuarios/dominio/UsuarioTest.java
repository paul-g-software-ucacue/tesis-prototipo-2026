package ec.mdd.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void dadoUnUsuarioActivo_cuandoSePreguntaSiPuedeIniciarSesion_entoncesSi() {
        assertThat(
                        new Usuario("autor.prueba", "hash", true, Set.of("DOCENTE"))
                                .puedeIniciarSesion())
                .isTrue();
    }

    @Test
    void dadoUnUsuarioInactivo_cuandoSePreguntaSiPuedeIniciarSesion_entoncesNo() {
        assertThat(new Usuario("autor.prueba", "hash", false, Set.of()).puedeIniciarSesion())
                .isFalse();
    }

    @Test
    void dadoUnUsuario_cuandoSeConvierteEnTexto_entoncesNoMuestraElHash() {
        Usuario usuario = new Usuario("autor.prueba", "hash-secreto", true, null);

        assertThat(usuario.toString()).doesNotContain("hash-secreto").contains("autor.prueba");
        assertThat(usuario.roles()).isEmpty();
    }
}
