package ec.mdd.usuarios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.dominio.Usuario;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** La carga del usuario de prueba con dobles escritos a mano de los puertos (sin Mockito). */
class CargaUsuarioDePruebaTest {

    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
    private final VerificadorFalso verificador = new VerificadorFalso();

    @Test
    void dadoQueEstaDeshabilitado_cuandoArranca_entoncesNoCreaNada() {
        carga(false, "una-clave").run(null);

        assertThat(repositorio.usuarios).isEmpty();
    }

    @Test
    void dadoQueEstaDeshabilitadoYElUsuarioYaExiste_cuandoArranca_entoncesLoDesactiva() {
        repositorio.guardar(
                new Usuario(
                        CargaUsuarioDePrueba.NOMBRE,
                        "Autor de prueba",
                        "hash:vieja",
                        true,
                        CargaUsuarioDePrueba.ROLES));

        carga(false, null).run(null);

        Usuario usuario = repositorio.usuarios.get(0);
        assertThat(usuario.puedeIniciarSesion()).isFalse();
        assertThat(usuario.roles()).isEqualTo(CargaUsuarioDePrueba.ROLES);
    }

    @Test
    void dadoQueEstaDeshabilitadoYYaEstaInactivo_cuandoArranca_entoncesNoLoVuelveAGuardar() {
        repositorio.guardar(
                new Usuario(
                        CargaUsuarioDePrueba.NOMBRE, "Autor de prueba", "hash:x", false, Set.of()));

        carga(false, null).run(null);

        assertThat(repositorio.actualizaciones).isZero();
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
        assertThat(usuario.nombreParaMostrar()).isEqualTo("Autor de prueba");
        assertThat(usuario.claveHash()).isEqualTo("hash:una-clave");
        assertThat(usuario.roles()).containsExactlyInAnyOrder("DOCENTE", "COORDINADOR_CARRERA");
        assertThat(usuario.puedeIniciarSesion()).isTrue();
    }

    private CargaUsuarioDePrueba carga(boolean habilitado, String clave) {
        return new CargaUsuarioDePrueba(
                repositorio, verificador, new PropiedadesUsuarios.UsuarioPrueba(habilitado, clave));
    }
}
