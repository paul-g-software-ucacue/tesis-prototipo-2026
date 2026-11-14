package ec.mdd.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.usuarios.infraestructura.UsuariosFlywayConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Migra solo este servicio contra un PostgreSQL vacío y comprueba que todas las tablas que crea
 * quedan en su esquema (escenario «Un esquema por servicio», FR-011).
 *
 * <p>Como la base solo tiene las migraciones de este servicio, cualquier tabla fuera de su esquema
 * es suya. Así se atribuye sin ambigüedad, cosa que la prueba del proceso no puede hacer porque un
 * mismo rol es dueño de los esquemas de todos sus servicios.
 */
class UsuariosEsquemaIntegrationTest {

    private static final String TABLAS_DE_USUARIO =
            "select table_schema, table_name from information_schema.tables"
                    + " where table_schema not in ('pg_catalog', 'information_schema')";

    @Test
    void dadoUnaBaseVacia_cuandoElServicioMigra_entoncesTodasSusTablasQuedanEnSuEsquema()
            throws SQLException {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

            new UsuariosFlywayConfig().flywayUsuarios(dataSource).migrate();

            List<String> tablas = tablasDeUsuario(dataSource);
            assertThat(tablas)
                    .contains(
                            "usuarios.flyway_schema_history",
                            "usuarios.evento_recibido",
                            "usuarios.intento_fallido");
            assertThat(tablas).allMatch(tabla -> tabla.startsWith("usuarios."));
        }
    }

    @Test
    void dadasDosCuentas_cuandoTienenLaMismaIdentificacion_entoncesSeRechazaLaSegunda() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new UsuariosFlywayConfig().flywayUsuarios(dataSource).migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);

            cuenta(jdbc, "sin.uno", null);
            cuenta(jdbc, "sin.dos", null);
            cuenta(jdbc, "vicerrectorado.demo", "0109990028");

            assertThatThrownBy(() -> cuenta(jdbc, "otra.cuenta", "0109990028"))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> cuenta(jdbc, "mal.formada", "abc-1"))
                    .isInstanceOf(DataAccessException.class);
        }
    }

    private static void cuenta(JdbcClient jdbc, String usuario, String identificacion) {
        jdbc.sql(
                        "insert into usuarios.usuario (id, nombre_usuario, clave_hash, activo,"
                                + " nombre_para_mostrar, identificacion) values (:id, :usuario,"
                                + " 'x', true, :usuario, :identificacion)")
                .param("id", UUID.randomUUID())
                .param("usuario", usuario)
                .param("identificacion", identificacion)
                .update();
    }

    private static List<String> tablasDeUsuario(DataSource dataSource) throws SQLException {
        List<String> tablas = new ArrayList<>();
        try (Connection conexion = dataSource.getConnection();
                Statement consulta = conexion.createStatement();
                ResultSet filas = consulta.executeQuery(TABLAS_DE_USUARIO)) {
            while (filas.next()) {
                tablas.add(filas.getString(1) + "." + filas.getString(2));
            }
        }
        return tablas;
    }
}
