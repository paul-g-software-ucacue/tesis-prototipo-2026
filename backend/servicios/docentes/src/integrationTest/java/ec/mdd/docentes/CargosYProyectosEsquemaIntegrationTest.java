package ec.mdd.docentes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.docentes.infraestructura.DocentesFlywayConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * V3: las tablas de los cargos y de los proyectos rechazan lo que está fuera de las reglas de las
 * cargas 8 y 9, también un segundo coordinador vigente por carrera (RN-12), y el registro admite
 * sus cuatro acciones (F03-H05, data-model.md).
 */
class CargosYProyectosEsquemaIntegrationTest {

    private static PostgreSQLContainer postgres;
    private static JdbcClient jdbc;

    @BeforeAll
    static void migrar() {
        postgres = new PostgreSQLContainer("postgres:18.6-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new DocentesFlywayConfig().flywayDocentes(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        for (String docente : List.of("0109990010", "0109990028", "0109990044")) {
            jdbc.sql(
                            "INSERT INTO docentes.docente (identificacion, tipo_identificacion,"
                                    + " primer_apellido, primer_nombre, correo_institucional,"
                                    + " tipo_personal, categoria_escalafon, dedicacion,"
                                    + " horas_contratadas, fecha_ingreso, inicio_contrato,"
                                    + " condicion_especial, carrera_base, titulo_tercer_nivel,"
                                    + " campo_amplio, campo_especifico, estado, registrado_en,"
                                    + " registrado_por) VALUES (:id, 'CEDULA', 'PERALTA', 'JUAN',"
                                    + " :correo, 'TITULAR', 'AUXILIAR_1', 'TC', 40, DATE"
                                    + " '2019-10-01', DATE '2026-04-01', 'NINGUNA', '550613A01-P-0301',"
                                    + " 'INGENIERO', '06', '061', 'ACTIVO', now(), 'datos.demo')")
                    .param("id", docente)
                    .param("correo", docente + "@prueba.edu.ec")
                    .update();
        }
    }

    @AfterAll
    static void detener() {
        postgres.stop();
    }

    @Test
    void dadaLaTablaDeLosCargos_cuandoSeGuardaAlgoFueraDeSusReglas_entoncesLaBaseLoRechaza() {
        assertThat(cargo(Map.of("fecha_designacion", "2020-01-01"))).isEqualTo(1);
        List<Map<String, Object>> invalidos =
                List.of(
                        Map.of("tipo", "DECANO"),
                        Map.of("estado", "SUSPENDIDO"),
                        // El fin igual a la designación, un día antes y 2 años y un día después.
                        Map.of("fecha_designacion", "2019-01-01", "fecha_fin", "2019-01-01"),
                        Map.of("fecha_designacion", "2019-02-01", "fecha_fin", "2019-01-31"),
                        Map.of("fecha_designacion", "2019-03-01", "fecha_fin", "2021-03-02"),
                        Map.of("documento", " "),
                        Map.of("unidad", "rect"),
                        // Un rector sin unidad, y un coordinador de carrera sin carrera.
                        Map.of("unidad", ""),
                        Map.of("tipo", "COORDINADOR_CARRERA", "unidad", "", "carrera", ""),
                        // Un rector con carrera.
                        Map.of("carrera", "550613A01-P-0301"),
                        // Un docente que no está en la tabla de los docentes.
                        Map.of("docente", "0109999904"));
        for (int i = 0; i < invalidos.size(); i++) {
            // Cada caso con su propia fecha de designación, para que solo falle por lo que prueba.
            Map<String, Object> caso = new HashMap<>(invalidos.get(i));
            caso.putIfAbsent("fecha_designacion", "2021-01-" + String.format("%02d", i + 1));
            assertThatThrownBy(() -> cargo(caso)).as(caso.toString()).isInstanceOf(Exception.class);
        }
        // Exactamente 2 años después pasa.
        assertThat(cargo(Map.of("fecha_designacion", "2022-01-01", "fecha_fin", "2024-01-01")))
                .isEqualTo(1);
        // Un coordinador de carrera sin unidad pasa.
        assertThat(coordinador("0109990010", "C1", "VIGENTE")).isEqualTo(1);
    }

    @Test
    void dadaUnaCarreraConCoordinadorVigente_cuandoSeGuardaOtroVigente_entoncesLaBaseLoRechaza() {
        assertThat(coordinador("0109990028", "C2", "VIGENTE")).isEqualTo(1);
        assertThat(coordinador("0109990044", "C2", "FINALIZADO")).isEqualTo(1);

        assertThatThrownBy(() -> coordinador("0109990044", "C2", "VIGENTE", "2025-05-01"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void dadaLaTablaDeLosProyectos_cuandoSeGuardaAlgoFueraDeSusReglas_entoncesLaBaseLoRechaza() {
        assertThat(proyecto(Map.of("codigo", "INV-2025-01"))).isEqualTo(1);
        List<Map<String, Object>> invalidos =
                List.of(
                        Map.of("codigo", "inv-1"),
                        Map.of("tipo", "DOCENCIA"),
                        Map.of("estado", "CERRADO"),
                        Map.of("vigencia_fin", "2025-04-01"),
                        Map.of("horas", 0),
                        Map.of("horas", 1000),
                        Map.of("carreras", "[]"),
                        Map.of("participantes", "{}"),
                        Map.of("resolucion", " "),
                        Map.of("director", "0109999904"),
                        // El código de otro proyecto.
                        Map.of("codigo", "INV-2025-01"));
        for (int i = 0; i < invalidos.size(); i++) {
            Map<String, Object> caso = new HashMap<>(invalidos.get(i));
            caso.putIfAbsent("codigo", "P-" + i);
            assertThatThrownBy(() -> proyecto(caso))
                    .as(caso.toString())
                    .isInstanceOf(Exception.class);
        }
        assertThat(proyecto(Map.of("codigo", "P-HORAS", "horas", 999.99))).isEqualTo(1);
    }

    @Test
    void dadoElRegistro_cuandoSeGuardanLasAccionesDeF03H04YF03H05_entoncesLasAdmite() {
        for (String accion :
                List.of(
                        "DOCENTE_REGISTRADO",
                        "DOCENTE_CORREGIDO",
                        "DOCENTE_ESTADO_CAMBIADO",
                        "CARGO_REGISTRADO",
                        "CARGO_CORREGIDO",
                        "PROYECTO_REGISTRADO",
                        "PROYECTO_CORREGIDO")) {
            assertThat(cambio(accion)).as(accion).isEqualTo(1);
        }
        assertThatThrownBy(() -> cambio("CARGO_BORRADO")).isInstanceOf(Exception.class);
    }

    // Un rector válido, con los valores dados en lugar de los suyos.
    private static int cargo(Map<String, Object> cambios) {
        Map<String, Object> valores = new HashMap<>();
        valores.put("docente", "0109990010");
        valores.put("tipo", "RECTOR");
        valores.put("unidad", "RECT");
        valores.put("carrera", "");
        valores.put("fecha_fin", "");
        valores.put("documento", "RES-PRUEBA-2025-001");
        valores.put("estado", "VIGENTE");
        valores.putAll(cambios);
        return jdbc.sql(
                        "INSERT INTO docentes.cargo (docente, tipo, fecha_designacion, unidad,"
                                + " carrera, fecha_fin, documento, estado, registrado_en,"
                                + " registrado_por) VALUES (:docente, :tipo,"
                                + " CAST(:fecha_designacion AS date), NULLIF(:unidad, ''),"
                                + " NULLIF(:carrera, ''), CAST(NULLIF(:fecha_fin, '') AS date),"
                                + " :documento, :estado, now(), 'datos.demo')")
                .params(valores)
                .update();
    }

    private static int coordinador(String docente, String carrera, String estado) {
        return coordinador(docente, carrera, estado, "2025-04-01");
    }

    private static int coordinador(
            String docente, String carrera, String estado, String designacion) {
        return cargo(
                Map.of(
                        "docente", docente,
                        "tipo", "COORDINADOR_CARRERA",
                        "unidad", "",
                        "carrera", carrera,
                        "estado", estado,
                        "fecha_designacion", designacion));
    }

    // Un proyecto válido, con los valores dados en lugar de los suyos.
    private static int proyecto(Map<String, Object> cambios) {
        Map<String, Object> valores = new HashMap<>();
        valores.put("tipo", "INVESTIGACION");
        valores.put("carreras", "[\"550613A01-P-0301\"]");
        valores.put("resolucion", "RES-OCS-PRUEBA-2025-040");
        valores.put("estado", "EN_EJECUCION");
        valores.put("vigencia_fin", "2026-12-31");
        valores.put("director", "0109990010");
        valores.put(
                "participantes",
                "[{\"docente\": \"0109990010\", \"rol\": \"DIRECTOR\", \"horas\": 14}]");
        valores.put("horas", 24);
        valores.putAll(cambios);
        return jdbc.sql(
                        "INSERT INTO docentes.proyecto (codigo, nombre, tipo, carreras,"
                                + " resolucion_ocs, fecha_resolucion, estado, vigencia_inicio,"
                                + " vigencia_fin, director, participantes, horas_aprobadas,"
                                + " registrado_en, registrado_por) VALUES (:codigo, 'Prototipo',"
                                + " :tipo, CAST(:carreras AS jsonb), :resolucion, DATE"
                                + " '2025-03-20', :estado, DATE '2025-04-01',"
                                + " CAST(:vigencia_fin AS date), :director,"
                                + " CAST(:participantes AS jsonb), :horas, now(), 'datos.demo')")
                .params(valores)
                .update();
    }

    private static int cambio(String accion) {
        return jdbc.sql(
                        "INSERT INTO docentes.registro_cambio (id, momento, usuario, accion,"
                                + " detalle) VALUES (:id, now(), 'datos.demo', :accion,"
                                + " CAST('{}' AS jsonb))")
                .param("id", UUID.randomUUID())
                .param("accion", accion)
                .update();
    }
}
