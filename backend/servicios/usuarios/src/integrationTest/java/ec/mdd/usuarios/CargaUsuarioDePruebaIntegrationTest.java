package ec.mdd.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.adaptadores.salida.RepositorioUsuariosJpa;
import ec.mdd.usuarios.adaptadores.salida.UsuarioJpaRepositorio;
import ec.mdd.usuarios.adaptadores.salida.VerificadorArgon2;
import ec.mdd.usuarios.infraestructura.CargaUsuarioDePrueba;
import ec.mdd.usuarios.infraestructura.PropiedadesUsuarios;
import ec.mdd.usuarios.infraestructura.UsuariosFlywayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** El usuario de prueba se crea una sola vez en PostgreSQL, con su contraseña en Argon2id. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UsuariosFlywayConfig.class, CargaUsuarioDePruebaIntegrationTest.Contenedores.class})
class CargaUsuarioDePruebaIntegrationTest {

    @Autowired private UsuarioJpaRepositorio filas;

    @Test
    void dadoQueEstaHabilitado_cuandoArrancaDosVeces_entoncesHayUnSoloUsuarioDePrueba() {
        RepositorioUsuariosJpa repositorio = new RepositorioUsuariosJpa(filas);
        VerificadorArgon2 verificador =
                new VerificadorArgon2(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        AutorizacionAnotada autorizacion = new AutorizacionAnotada();
        CargaUsuarioDePrueba carga =
                new CargaUsuarioDePrueba(
                        repositorio,
                        verificador,
                        autorizacion,
                        new PropiedadesUsuarios.UsuarioPrueba(true, "clave-generada-en-la-prueba"));

        carga.run(null);
        carga.run(null);

        assertThat(filas.count()).isEqualTo(1);
        var usuario = repositorio.buscarPorNombre(CargaUsuarioDePrueba.NOMBRE).orElseThrow();
        assertThat(verificador.coincide("clave-generada-en-la-prueba", usuario.claveHash()))
                .isTrue();
        assertThat(autorizacion.pedidos)
                .containsExactly(
                        java.util.Map.entry(
                                CargaUsuarioDePrueba.NOMBRE,
                                java.util.List.of("COORDINADOR_CARRERA", "DOCENTE")));
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
