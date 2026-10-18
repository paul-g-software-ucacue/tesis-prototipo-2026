package ec.mdd.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.usuarios.adaptadores.salida.RepositorioUsuariosJpa;
import ec.mdd.usuarios.adaptadores.salida.UsuarioJpaRepositorio;
import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.infraestructura.UsuariosFlywayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * El repositorio de Usuarios contra PostgreSQL real, con las migraciones de Usuarios (principio
 * VIII).
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UsuariosFlywayConfig.class, RepositorioUsuariosJpaIntegrationTest.Contenedores.class})
class RepositorioUsuariosJpaIntegrationTest {

    @Autowired private UsuarioJpaRepositorio filas;

    @Test
    void dadoUnUsuario_cuandoSeGuardaYSeBusca_entoncesConservaSusDatos() {
        RepositorioUsuariosJpa repositorio = new RepositorioUsuariosJpa(filas);
        Usuario usuario = new Usuario("autor.prueba", "Autor de prueba", "hash", true);

        repositorio.guardar(usuario);

        assertThat(repositorio.buscarPorNombre("autor.prueba")).contains(usuario);
        assertThat(repositorio.buscarPorNombre(" Autor.PRUEBA ")).contains(usuario);
        assertThat(repositorio.buscarPorNombre("nadie")).isEmpty();
    }

    @Test
    void dadoVariosUsuarios_cuandoSeListanTodos_entoncesVienenPorNombreDeUsuario() {
        RepositorioUsuariosJpa repositorio = new RepositorioUsuariosJpa(filas);
        repositorio.guardar(new Usuario("zeta.demo", "Zeta", "hash", true));
        repositorio.guardar(new Usuario("alfa.demo", "Alfa", "hash", false));

        assertThat(repositorio.todos())
                .extracting(Usuario::nombreUsuario)
                .containsSubsequence("alfa.demo", "zeta.demo")
                .isSorted();
    }

    @Test
    void dadoUnUsuarioGuardado_cuandoSeActualiza_entoncesCambiaSusDatosSinDuplicarlo() {
        RepositorioUsuariosJpa repositorio = new RepositorioUsuariosJpa(filas);
        repositorio.guardar(new Usuario("docente.demo", "Viejo", "hash", false));
        Usuario nuevo = new Usuario("docente.demo", "Docente (demo)", "otro", true);

        repositorio.actualizar(nuevo);
        filas.flush();

        assertThat(repositorio.buscarPorNombre("docente.demo")).contains(nuevo);
        assertThat(filas.count()).isEqualTo(1);
        assertThatThrownBy(() -> repositorio.actualizar(new Usuario("nadie", "Nadie", "h", true)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dadoUnNombreRepetido_cuandoSeGuarda_entoncesLaBaseLoRechaza() {
        RepositorioUsuariosJpa repositorio = new RepositorioUsuariosJpa(filas);
        repositorio.guardar(new Usuario("repetido", "Repetido", "hash", true));

        assertThatThrownBy(
                        () -> {
                            repositorio.guardar(new Usuario("Repetido", "Otro", "otro", true));
                            filas.flush();
                        })
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Contenedores {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18.6-alpine");
        }
    }
}
