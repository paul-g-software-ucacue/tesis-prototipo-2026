package ec.mdd.docentes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.docentes.infraestructura.DocentesFlywayConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
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
class DocentesEsquemaIntegrationTest {

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

            new DocentesFlywayConfig().flywayDocentes(dataSource).migrate();

            List<String> tablas = tablasDeUsuario(dataSource);
            assertThat(tablas).contains("docentes.flyway_schema_history");
            assertThat(tablas).allMatch(tabla -> tabla.startsWith("docentes."));
            assertThat(tablas).contains("docentes.docente", "docentes.registro_cambio");
        }
    }

    @Test
    void dadaLaTablaDeLosDocentes_cuandoSeGuardaAlgoFueraDeSusReglas_entoncesLaBaseLoRechaza() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new DocentesFlywayConfig().flywayDocentes(dataSource).migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);

            assertThat(docente(jdbc, Map.of())).isEqualTo(1);
            List<Map<String, Object>> invalidos =
                    List.of(
                            Map.of("tipo_identificacion", "RUC"),
                            Map.of("tipo_personal", "CONTRATADO"),
                            Map.of("categoria_escalafon", "AUXILIAR_3"),
                            Map.of("dedicacion", "XX"),
                            Map.of("condicion_especial", "OTRA"),
                            Map.of("estado", "JUBILADO"),
                            Map.of("horas_contratadas", 0),
                            Map.of("horas_contratadas", 41),
                            Map.of("fin_contrato", "2026-04-01"),
                            Map.of("campo_especifico", "071"),
                            Map.of("telefono", "099"),
                            Map.of("cursos", "{}"),
                            // El correo de otro docente, con otras mayúsculas.
                            Map.of("correo", "JUAN@PRUEBA.EDU.EC"),
                            // La identificación de otro docente.
                            Map.of("identificacion", "0109990010"));
            for (int i = 0; i < invalidos.size(); i++) {
                // Cada caso con su propia identificación y su propio correo, para que solo falle
                // por el valor que prueba.
                Map<String, Object> caso = new java.util.HashMap<>(invalidos.get(i));
                caso.putIfAbsent("identificacion", "P" + i);
                caso.putIfAbsent("correo", "caso" + i + "@prueba.edu.ec");
                assertThatThrownBy(() -> docente(jdbc, caso))
                        .as(caso.toString())
                        .isInstanceOf(Exception.class);
            }
            assertThat(
                            docente(
                                    jdbc,
                                    Map.of(
                                            "identificacion",
                                            "AB12345",
                                            "correo",
                                            "x@prueba.edu.ec")))
                    .isEqualTo(1);

            for (String accion :
                    List.of("DOCENTE_REGISTRADO", "DOCENTE_CORREGIDO", "DOCENTE_ESTADO_CAMBIADO")) {
                assertThat(cambio(jdbc, accion)).isEqualTo(1);
            }
            assertThatThrownBy(() -> cambio(jdbc, "DOCENTE_BORRADO")).isInstanceOf(Exception.class);
        }
    }

    // Un docente válido de prueba, con los valores dados en lugar de los suyos.
    private static int docente(JdbcClient jdbc, Map<String, Object> cambios) {
        Map<String, Object> valores = new java.util.HashMap<>();
        valores.put("identificacion", "0109990010");
        valores.put("tipo_identificacion", "CEDULA");
        valores.put("correo", "juan@prueba.edu.ec");
        valores.put("telefono", "0990000001");
        valores.put("tipo_personal", "OCASIONAL");
        valores.put("categoria_escalafon", "NO_APLICA");
        valores.put("dedicacion", "TC");
        valores.put("horas_contratadas", 40);
        valores.put("fin_contrato", "2026-08-31");
        valores.put("condicion_especial", "NINGUNA");
        valores.put("campo_especifico", "061");
        valores.put("cursos", "[]");
        valores.put("estado", "ACTIVO");
        valores.putAll(cambios);
        return jdbc.sql(
                        "INSERT INTO docentes.docente (identificacion, tipo_identificacion,"
                                + " primer_apellido, primer_nombre, correo_institucional, telefono,"
                                + " tipo_personal, categoria_escalafon, dedicacion,"
                                + " horas_contratadas, fecha_ingreso, inicio_contrato, fin_contrato,"
                                + " condicion_especial, carrera_base, titulo_tercer_nivel,"
                                + " campo_amplio, campo_especifico, cursos, estado, registrado_en,"
                                + " registrado_por) VALUES (:identificacion, :tipo_identificacion,"
                                + " 'PERALTA', 'JUAN', :correo, :telefono, :tipo_personal,"
                                + " :categoria_escalafon, :dedicacion, :horas_contratadas,"
                                + " DATE '2019-10-01', DATE '2026-04-01', CAST(:fin_contrato AS date),"
                                + " :condicion_especial, '550613A01-P-0301', 'INGENIERO', '06',"
                                + " :campo_especifico, CAST(:cursos AS jsonb), :estado, now(),"
                                + " 'datos.demo')")
                .params(valores)
                .update();
    }

    private static int cambio(JdbcClient jdbc, String accion) {
        return jdbc.sql(
                        "INSERT INTO docentes.registro_cambio (id, momento, usuario, accion,"
                                + " detalle) VALUES (:id, now(), 'datos.demo', :accion,"
                                + " CAST('{}' AS jsonb))")
                .param("id", UUID.randomUUID())
                .param("accion", accion)
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
