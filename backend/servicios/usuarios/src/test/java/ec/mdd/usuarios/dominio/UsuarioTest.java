package ec.mdd.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void dadoUnUsuarioActivo_cuandoSePreguntaSiPuedeIniciarSesion_entoncesSi() {
        assertThat(
                        new Usuario("autor.prueba", "Autor", "hash", true, Set.of("DOCENTE"))
                                .puedeIniciarSesion())
                .isTrue();
    }

    @Test
    void dadoUnUsuarioInactivo_cuandoSePreguntaSiPuedeIniciarSesion_entoncesNo() {
        assertThat(
                        new Usuario("autor.prueba", "Autor", "hash", false, Set.of())
                                .puedeIniciarSesion())
                .isFalse();
    }

    @Test
    void dadoUnUsuario_cuandoSeConvierteEnTexto_entoncesNoMuestraElHash() {
        Usuario usuario = new Usuario("autor.prueba", "Autor", "hash-secreto", true, null);

        assertThat(usuario.toString()).doesNotContain("hash-secreto").contains("autor.prueba");
        assertThat(usuario.roles()).isEmpty();
    }

    @Test
    void dadoUnNombreConMayusculasYEspacios_cuandoSeNormaliza_entoncesQuedaEnMinusculas() {
        assertThat(Usuario.normalizar(" Coordinacion.Demo ")).isEqualTo("coordinacion.demo");
        assertThat(Usuario.normalizar(null)).isEmpty();
    }

    @Test
    void dadoUnNombreConMayusculas_cuandoSeCreaElUsuario_entoncesGuardaElNombreNormalizado() {
        Usuario usuario = new Usuario("Docente.Demo", "Docente (demo)", "hash", true, Set.of());

        assertThat(usuario.nombreUsuario()).isEqualTo("docente.demo");
    }

    @Test
    void dadoUnNombreParaMostrarVacio_cuandoSeCreaElUsuario_entoncesLoRechaza() {
        assertThatThrownBy(() -> new Usuario("docente.demo", " ", "hash", true, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Usuario("docente.demo", null, "hash", true, Set.of()))
                .isInstanceOf(NullPointerException.class);
    }
}
