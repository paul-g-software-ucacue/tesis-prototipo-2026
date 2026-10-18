package ec.mdd.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.adaptadores.salida.RepositorioUsuariosJpa;
import ec.mdd.usuarios.adaptadores.salida.UsuarioJpaRepositorio;
import ec.mdd.usuarios.adaptadores.salida.VerificadorArgon2;
import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.infraestructura.CargaUsuariosDemo;
import ec.mdd.usuarios.infraestructura.PropiedadesUsuarios;
import ec.mdd.usuarios.infraestructura.UsuariosFlywayConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Los 11 usuarios de la demo del archivo versionado en PostgreSQL, con Argon2id real (D-59): se
 * crean una vez y, al rotar una clave, se actualiza su hash sin duplicarlos.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UsuariosFlywayConfig.class, CargaUsuariosDemoIntegrationTest.Contenedores.class})
class CargaUsuariosDemoIntegrationTest {

    @Autowired private UsuarioJpaRepositorio filas;

    @TempDir Path carpeta;

    @Test
    void dadoElArchivoDeClaves_cuandoArrancaYLuegoRotaUnaClave_entoncesOnceUsuariosSinDuplicar()
            throws Exception {
        RepositorioUsuariosJpa repositorio = new RepositorioUsuariosJpa(filas);
        VerificadorArgon2 verificador =
                new VerificadorArgon2(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        Path claves = carpeta.resolve("claves-demo");
        StringBuilder lineas = new StringBuilder();
        for (CargaUsuariosDemo.UsuarioDemo demo : CargaUsuariosDemo.delArchivoVersionado()) {
            lineas.append(demo.usuario()).append("=clave-").append(demo.usuario()).append('\n');
        }
        Files.writeString(claves, lineas);
        PropiedadesUsuarios.Demo configuracion =
                new PropiedadesUsuarios.Demo(claves.toString(), null);

        AutorizacionAnotada autorizacion = new AutorizacionAnotada();
        cargar(repositorio, verificador, autorizacion, configuracion);
        Files.writeString(claves, lineas.toString().replace("clave-rectorado", "rotada"));
        cargar(repositorio, verificador, autorizacion, configuracion);
        filas.flush();

        assertThat(filas.count()).isEqualTo(11);
        Usuario rector = repositorio.buscarPorNombre("rectorado.demo").orElseThrow();
        assertThat(rector.claveHash()).startsWith("$argon2id$");
        assertThat(verificador.coincide("rotada.demo", rector.claveHash())).isTrue();
        Usuario mixto = repositorio.buscarPorNombre("docente.coordinador.demo").orElseThrow();
        assertThat(autorizacion.pedidos)
                .hasSize(11)
                .containsEntry(
                        "docente.coordinador.demo", List.of("COORDINADOR_CARRERA", "DOCENTE"));
        assertThat(verificador.coincide("clave-docente.coordinador.demo", mixto.claveHash()))
                .isTrue();
    }

    private static void cargar(
            RepositorioUsuariosJpa repositorio,
            VerificadorArgon2 verificador,
            AutorizacionAnotada autorizacion,
            PropiedadesUsuarios.Demo configuracion) {
        new CargaUsuariosDemo(
                        repositorio,
                        verificador,
                        autorizacion,
                        CargaUsuariosDemo.delArchivoVersionado(),
                        configuracion)
                .run(null);
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
