package ec.mdd.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.infraestructura.UsuariosFlywayConfig;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * La migración V4 de F05-H01 sobre datos de antes: nombres en minúsculas, el nombre para mostrar
 * con el nombre de usuario y la tabla de intentos (D-59, D-60).
 */
class MigracionUsuariosDemoIntegrationTest {

    @Test
    void dadoUnUsuarioConMayusculasDeAntes_cuandoSeMigraAV4_entoncesQuedaNormalizadoYConNombre() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            Flyway completa = new UsuariosFlywayConfig().flywayUsuarios(dataSource);
            Flyway.configure()
                    .configuration(completa.getConfiguration())
                    .target("3")
                    .load()
                    .migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);
            jdbc.sql(
                            "insert into usuarios.usuario (id, nombre_usuario, clave_hash, activo)"
                                    + " values (?, 'Autor.Prueba', 'hash', true)")
                    .param(UUID.randomUUID())
                    .update();

            completa.migrate();

            assertThat(
                            jdbc.sql(
                                            "select nombre_usuario || '|' || nombre_para_mostrar"
                                                    + " from usuarios.usuario")
                                    .query(String.class)
                                    .single())
                    .isEqualTo("autor.prueba|Autor.Prueba");
            assertThat(
                            jdbc.sql(
                                            "select is_nullable from information_schema.columns"
                                                    + " where table_schema = 'usuarios'"
                                                    + " and table_name = 'usuario'"
                                                    + " and column_name = 'nombre_para_mostrar'")
                                    .query(String.class)
                                    .single())
                    .isEqualTo("NO");
            assertThat(
                            jdbc.sql(
                                            "select count(*) from information_schema.columns"
                                                    + " where table_schema = 'usuarios'"
                                                    + " and table_name = 'intento_fallido'")
                                    .query(Long.class)
                                    .single())
                    .isEqualTo(4);
        }
    }
}
