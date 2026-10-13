package ec.mdd.usuarios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** La carga del usuario de prueba con dobles escritos a mano de los puertos (sin Mockito). */
class CargaUsuarioDePruebaTest {

    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
    private final VerificadorDeClaves verificador = new VerificadorFalso();

    @Test
    void dadoQueEstaDeshabilitado_cuandoArranca_entoncesNoCreaNada() {
        carga(false, "una-clave").run(null);

        assertThat(repositorio.usuarios).isEmpty();
    }

    @Test
    void dadoQueEstaHabilitadoSinContrasena_cuandoArranca_entoncesNoCreaNada() {
        carga(true, " ").run(null);

        assertThat(repositorio.usuarios).isEmpty();
    }

    @Test
    void dadoQueEstaHabilitado_cuandoArrancaDosVeces_entoncesCreaElUsuarioUnaSolaVez() {
        CargaUsuarioDePrueba carga = carga(true, "una-clave");

        carga.run(null);
        carga.run(null);

        assertThat(repositorio.usuarios).hasSize(1);
        Usuario usuario = repositorio.usuarios.get(0);
        assertThat(usuario.nombreUsuario()).isEqualTo(CargaUsuarioDePrueba.NOMBRE);
        assertThat(usuario.claveHash()).isEqualTo("hash:una-clave");
        assertThat(usuario.roles()).containsExactlyInAnyOrder("DOCENTE", "COORDINADOR_CARRERA");
        assertThat(usuario.puedeIniciarSesion()).isTrue();
    }

    private CargaUsuarioDePrueba carga(boolean habilitado, String clave) {
        return new CargaUsuarioDePrueba(
                repositorio, verificador, new PropiedadesUsuarios.UsuarioPrueba(habilitado, clave));
    }

    private static final class RepositorioEnMemoria implements RepositorioUsuarios {

        private final List<Usuario> usuarios = new ArrayList<>();

        @Override
        public Optional<Usuario> buscarPorNombre(String nombreUsuario) {
            return usuarios.stream()
                    .filter(u -> u.nombreUsuario().equals(nombreUsuario))
                    .findFirst();
        }

        @Override
        public void guardar(Usuario usuario) {
            usuarios.add(usuario);
        }
    }

    private static final class VerificadorFalso implements VerificadorDeClaves {

        @Override
        public String calcularHash(String clave) {
            return "hash:" + clave;
        }

        @Override
        public boolean coincide(String clave, String hash) {
            return hash.equals("hash:" + clave);
        }
    }
}
