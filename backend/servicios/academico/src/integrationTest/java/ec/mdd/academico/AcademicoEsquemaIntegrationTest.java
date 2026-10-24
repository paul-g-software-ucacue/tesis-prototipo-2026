package ec.mdd.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.academico.infraestructura.AcademicoFlywayConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Migra solo este servicio contra un PostgreSQL vacío y comprueba que todas las tablas que crea
 * quedan en su esquema (escenario «Un esquema por servicio», FR-011), y que las tablas de los
 * periodos (F02-H01) y de la estructura orgánica (F03-H01) guardan sus reglas también en la base
 * (data-model.md).
 *
 * <p>Como la base solo tiene las migraciones de este servicio, cualquier tabla fuera de su esquema
 * es suya. Así se atribuye sin ambigüedad, cosa que la prueba del proceso no puede hacer porque un
 * mismo rol es dueño de los esquemas de todos sus servicios.
 */
class AcademicoEsquemaIntegrationTest {

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

            new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();

            List<String> tablas = tablasDeUsuario(dataSource);
            assertThat(tablas).contains("academico.flyway_schema_history");
            assertThat(tablas).allMatch(tabla -> tabla.startsWith("academico."));
            assertThat(tablas)
                    .contains(
                            "academico.periodo_academico",
                            "academico.registro_cambio",
                            "academico.gestion_autorizada",
                            "academico.unidad_organica",
                            "academico.campo_amplio",
                            "academico.carrera",
                            "academico.malla",
                            "academico.asignatura",
                            "academico.asignatura_paralelo");
        }
    }

    @Test
    void dadoLasTablasDeLosPeriodos_cuandoSeGuardaAlgoFueraDeSusReglas_entoncesLaBaseLoRechaza() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            DataSource dataSource =
                    new DriverManagerDataSource(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();
            JdbcClient jdbc = JdbcClient.create(dataSource);

            assertThat(periodo(jdbc, "2026-2", "Periodo 2026-2", "2027-02-28", "PLANIFICACION"))
                    .isEqualTo(1);
            for (String[] invalido :
                    new String[][] {
                        {"2026 3", "Periodo", "2027-02-28", "PLANIFICACION"},
                        {"2026-3", "  ", "2027-02-28", "PLANIFICACION"},
                        {"2026-3", "Periodo", "2026-09-01", "PLANIFICACION"},
                        {"2026-3", "Periodo", "2027-02-28", "ABIERTO"},
                        {"2026-2", "Repetido", "2027-02-28", "PLANIFICACION"}
                    }) {
                assertThatThrownBy(
                                () ->
                                        periodo(
                                                jdbc,
                                                invalido[0],
                                                invalido[1],
                                                invalido[2],
                                                invalido[3]))
                        .isInstanceOf(DataIntegrityViolationException.class);
            }
            assertThatThrownBy(
                            () ->
                                    jdbc.sql(
                                                    "INSERT INTO academico.registro_cambio"
                                                            + " (id, momento, usuario, accion,"
                                                            + " detalle) VALUES"
                                                            + " (gen_random_uuid(), now(), 'x',"
                                                            + " 'PERIODO_BORRADO', '{}')")
                                            .update())
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Test
    void dadaLaMigracion_cuandoSeLeeElCatalogo_entoncesTraeLas21GestionesDePol6() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            JdbcClient jdbc = migrada(postgres);

            List<String> codigos =
                    jdbc.sql("SELECT codigo FROM academico.gestion_autorizada ORDER BY codigo")
                            .query(String.class)
                            .list();
            assertThat(codigos).hasSize(21).startsWith("G01").endsWith("G21");
            assertThat(nombre(jdbc, "G01")).isEqualTo("Rector");
            assertThat(nombre(jdbc, "G07"))
                    .isEqualTo("Gestor de Investigación, Desarrollo Tecnológico e Innovación");
            assertThat(nombre(jdbc, "G21")).isEqualTo("Gestor Unidad de Estadísticas");
        }
    }

    @Test
    void dadaLaTablaDeLasUnidades_cuandoSeGuardaAlgoFueraDeSusReglas_entoncesLaBaseLoRechaza() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            JdbcClient jdbc = migrada(postgres);

            assertThat(unidad(jdbc, "MATRIZ", "SEDE_MATRIZ", null, null, null)).isEqualTo(1);
            assertThat(unidad(jdbc, "RECT", "GOBIERNO", "MATRIZ", "G01", null)).isEqualTo(1);
            assertThat(unidad(jdbc, "G-INV", "GESTION", "RECT", "G07", null)).isEqualTo(1);
            assertThat(unidad(jdbc, "COM-X", "COMISION", "RECT", null, true)).isEqualTo(1);
            for (Object[] invalida :
                    new Object[][] {
                        {"MATRIZ2", "SEDE_MATRIZ", null, null, null},
                        {"SEDE-M", "SEDE_MATRIZ", "MATRIZ", null, null},
                        {"OTRA", "SEDE", null, null, null},
                        {"G-SIN", "GESTION", "RECT", null, null},
                        {"G-X", "GESTION", "RECT", "G99", null},
                        {"COM-G", "COMISION", "RECT", "G07", true},
                        {"COM-S", "COMISION", "RECT", null, null},
                        {"G-P", "GESTION", "RECT", "G07", true},
                        {"HUERFANA", "GOBIERNO", "NADA", null, null},
                        {"g-min", "GOBIERNO", "RECT", null, null},
                        {"TIPO", "OFICINA", "RECT", null, null},
                        {"G-INV", "GOBIERNO", "RECT", null, null}
                    }) {
                assertThatThrownBy(
                                () ->
                                        unidad(
                                                jdbc,
                                                (String) invalida[0],
                                                (String) invalida[1],
                                                (String) invalida[2],
                                                (String) invalida[3],
                                                (Boolean) invalida[4]))
                        .as("unidad %s", invalida[0])
                        .isInstanceOf(DataIntegrityViolationException.class);
            }
            for (String accion :
                    new String[] {
                        "UNIDAD_REGISTRADA",
                        "UNIDAD_CORREGIDA",
                        "UNIDAD_INACTIVADA",
                        "UNIDAD_REACTIVADA",
                        "PERIODO_REGISTRADO"
                    }) {
                assertThat(
                                jdbc.sql(
                                                "INSERT INTO academico.registro_cambio (id, momento,"
                                                        + " usuario, accion, detalle) VALUES"
                                                        + " (gen_random_uuid(), now(), 'x', :accion,"
                                                        + " '{}')")
                                        .param("accion", accion)
                                        .update())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void dadaLaMigracion_cuandoSeLeenLosCamposAmplios_entoncesTraeLos11DeCine() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            JdbcClient jdbc = migrada(postgres);

            assertThat(
                            jdbc.sql("SELECT codigo FROM academico.campo_amplio ORDER BY codigo")
                                    .query(String.class)
                                    .list())
                    .hasSize(11)
                    .startsWith("00")
                    .endsWith("10");
            assertThat(
                            jdbc.sql(
                                            "SELECT nombre FROM academico.campo_amplio WHERE codigo = '06'")
                                    .query(String.class)
                                    .single())
                    .isEqualTo("Tecnologías de la información y la comunicación");
        }
    }

    @Test
    void
            dadasLasTablasDeCarrerasMallasYAsignaturas_cuandoSeGuardaAlgoFueraDeSusReglas_entoncesLaBaseLoRechaza() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            JdbcClient jdbc = migrada(postgres);
            assertThat(unidad(jdbc, "MATRIZ", "SEDE_MATRIZ", null, null, null)).isEqualTo(1);

            assertThat(carrera(jdbc, "C1", "DS", "{NOCTURNA}", "06", "061", 5)).isEqualTo(1);
            assertThat(carrera(jdbc, "C2", "CO", "{MATUTINA,VESPERTINA}", "04", "041", 5))
                    .isEqualTo(1);
            assertThat(malla(jdbc, "M1", "C1")).isEqualTo(1);
            assertThat(malla(jdbc, "M2", "C2")).isEqualTo(1);
            assertThat(asignatura(jdbc, "A1", "C1", "M1", "3", "{uno,dos}")).isEqualTo(1);
            for (Object[] invalida :
                    new Object[][] {
                        {"C3", "DS", "{NOCTURNA}", "06", "061", 5},
                        {"C3", "D3", "{}", "06", "061", 5},
                        {"C3", "D3", "{NOCHE}", "06", "061", 5},
                        {"C3", "D3", "{NOCTURNA}", "06", "071", 5},
                        {"C3", "D3", "{NOCTURNA}", "11", "111", 5},
                        {"C3", "D3", "{NOCTURNA}", "06", "061", 0},
                        {"c3", "D3", "{NOCTURNA}", "06", "061", 5}
                    }) {
                assertThatThrownBy(
                                () ->
                                        carrera(
                                                jdbc,
                                                (String) invalida[0],
                                                (String) invalida[1],
                                                (String) invalida[2],
                                                (String) invalida[3],
                                                (String) invalida[4],
                                                (Integer) invalida[5]))
                        .as("carrera %s %s %s", invalida[0], invalida[1], invalida[2])
                        .isInstanceOf(DataIntegrityViolationException.class);
            }
            assertThatThrownBy(() -> malla(jdbc, "M3", "C9"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            for (Object[] invalida :
                    new Object[][] {
                        {"A2", "C1", "M2", "3", "{uno,dos}"},
                        {"A2", "C1", "M1", "0.4", "{uno,dos}"},
                        {"A2", "C1", "M1", "3", "{uno}"},
                        {"A1", "C1", "M1", "3", "{uno,dos}"}
                    }) {
                assertThatThrownBy(
                                () ->
                                        asignatura(
                                                jdbc,
                                                (String) invalida[0],
                                                (String) invalida[1],
                                                (String) invalida[2],
                                                (String) invalida[3],
                                                (String) invalida[4]))
                        .as("asignatura %s %s %s", invalida[0], invalida[2], invalida[3])
                        .isInstanceOf(DataIntegrityViolationException.class);
            }
            for (String accion :
                    new String[] {
                        "CARRERA_REGISTRADA",
                        "CARRERA_CORREGIDA",
                        "CARRERA_ESTADO_CAMBIADO",
                        "MALLA_REGISTRADA",
                        "MALLA_ESTADO_CAMBIADO",
                        "ASIGNATURA_REGISTRADA",
                        "ASIGNATURA_CORREGIDA",
                        "ASIGNATURA_INACTIVADA",
                        "ASIGNATURA_REACTIVADA",
                        "UNIDAD_REACTIVADA",
                        "PERIODO_REGISTRADO"
                    }) {
                assertThat(
                                jdbc.sql(
                                                "INSERT INTO academico.registro_cambio (id, momento,"
                                                        + " usuario, accion, detalle) VALUES"
                                                        + " (gen_random_uuid(), now(), 'x', :accion,"
                                                        + " '{}')")
                                        .param("accion", accion)
                                        .update())
                        .as(accion)
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void dadaLaTablaDeLaOferta_cuandoSeGuardaAlgoFueraDeSusReglas_entoncesLaBaseLoRechaza() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6-alpine")) {
            postgres.start();
            JdbcClient jdbc = migrada(postgres);
            unidad(jdbc, "MATRIZ", "SEDE_MATRIZ", null, null, null);
            carrera(jdbc, "C1", "DS", "{NOCTURNA}", "06", "061", 5);
            malla(jdbc, "M1", "C1");
            asignatura(jdbc, "A1", "C1", "M1", "3", "{uno,dos}");
            jdbc.sql(
                            "INSERT INTO academico.periodo_academico (codigo, nombre, inicio, fin,"
                                    + " estado, registrado_en, registrado_por) VALUES ('2026-I',"
                                    + " 'Periodo', '2026-04-06', '2026-08-07', 'PLANIFICACION',"
                                    + " now(), 'vicerrectorado.demo')")
                    .update();

            assertThat(
                            ofertada(
                                    jdbc,
                                    "2026-I",
                                    "A1",
                                    "A_NOCTURNA",
                                    "NOCTURNA",
                                    "PRESENCIAL",
                                    28,
                                    null,
                                    "OFERTADA"))
                    .isEqualTo(1);
            assertThat(
                            ofertada(
                                    jdbc,
                                    "2026-I",
                                    "A1",
                                    "B_NOCTURNA",
                                    "NOCTURNA",
                                    "DUAL",
                                    0,
                                    "2.5",
                                    "RETIRADA"))
                    .isEqualTo(1);
            for (Object[] invalida :
                    new Object[][] {
                        {
                            "2026-I",
                            "A1",
                            "c_nocturna",
                            "NOCTURNA",
                            "PRESENCIAL",
                            28,
                            null,
                            "OFERTADA"
                        },
                        {"2026-I", "A1", "C_NOCTURNA", "NOCHE", "PRESENCIAL", 28, null, "OFERTADA"},
                        {"2026-I", "A1", "C_NOCTURNA", "NOCTURNA", "REMOTA", 28, null, "OFERTADA"},
                        {
                            "2026-I",
                            "A1",
                            "C_NOCTURNA",
                            "NOCTURNA",
                            "PRESENCIAL",
                            -1,
                            null,
                            "OFERTADA"
                        },
                        {
                            "2026-I",
                            "A1",
                            "C_NOCTURNA",
                            "NOCTURNA",
                            "PRESENCIAL",
                            28,
                            "0.4",
                            "OFERTADA"
                        },
                        {
                            "2026-I",
                            "A1",
                            "C_NOCTURNA",
                            "NOCTURNA",
                            "PRESENCIAL",
                            28,
                            null,
                            "BORRADA"
                        },
                        {
                            "2026-I",
                            "A1",
                            "A_NOCTURNA",
                            "NOCTURNA",
                            "PRESENCIAL",
                            28,
                            null,
                            "OFERTADA"
                        },
                        {
                            "2027-I",
                            "A1",
                            "C_NOCTURNA",
                            "NOCTURNA",
                            "PRESENCIAL",
                            28,
                            null,
                            "OFERTADA"
                        },
                        {
                            "2026-I",
                            "A9",
                            "C_NOCTURNA",
                            "NOCTURNA",
                            "PRESENCIAL",
                            28,
                            null,
                            "OFERTADA"
                        }
                    }) {
                assertThatThrownBy(
                                () ->
                                        ofertada(
                                                jdbc,
                                                (String) invalida[0],
                                                (String) invalida[1],
                                                (String) invalida[2],
                                                (String) invalida[3],
                                                (String) invalida[4],
                                                (Integer) invalida[5],
                                                (String) invalida[6],
                                                (String) invalida[7]))
                        .as(
                                "asignatura-paralelo %s %s %s %s",
                                invalida[0], invalida[1], invalida[2], invalida[3])
                        .isInstanceOf(DataIntegrityViolationException.class);
            }
            for (String accion :
                    new String[] {
                        "ASIGNATURA_PARALELO_REGISTRADA",
                        "ASIGNATURA_PARALELO_CORREGIDA",
                        "ASIGNATURA_PARALELO_RETIRADA",
                        "ASIGNATURA_PARALELO_REOFERTADA",
                        "ASIGNATURA_REACTIVADA",
                        "PERIODO_REGISTRADO"
                    }) {
                assertThat(
                                jdbc.sql(
                                                "INSERT INTO academico.registro_cambio (id, momento,"
                                                        + " usuario, accion, detalle) VALUES"
                                                        + " (gen_random_uuid(), now(), 'x', :accion,"
                                                        + " '{}')")
                                        .param("accion", accion)
                                        .update())
                        .as(accion)
                        .isEqualTo(1);
            }
        }
    }

    private static int ofertada(
            JdbcClient jdbc,
            String periodo,
            String asignatura,
            String paralelo,
            String jornada,
            String modalidad,
            int estudiantes,
            String horas,
            String estado) {
        return jdbc.sql(
                        "INSERT INTO academico.asignatura_paralelo (periodo, asignatura, paralelo,"
                                + " jornada, modalidad, sede, estudiantes, horas_semanales, estado,"
                                + " registrada_en, registrada_por) VALUES (:periodo, :asignatura,"
                                + " :paralelo, :jornada, :modalidad, null, :estudiantes,"
                                + " CAST(:horas AS numeric), :estado, now(), 'vicerrectorado.demo')")
                .param("periodo", periodo)
                .param("asignatura", asignatura)
                .param("paralelo", paralelo)
                .param("jornada", jornada)
                .param("modalidad", modalidad)
                .param("estudiantes", estudiantes)
                .param("horas", horas)
                .param("estado", estado)
                .update();
    }

    private static int carrera(
            JdbcClient jdbc,
            String codigo,
            String siglas,
            String jornadas,
            String amplio,
            String especifico,
            int ciclos) {
        return jdbc.sql(
                        "INSERT INTO academico.carrera (codigo, nombre, siglas, sede,"
                                + " nivel_formacion, modalidad, jornadas, campo_amplio,"
                                + " campo_especifico, campo_detallado, numero_ciclos, estado,"
                                + " registrada_en, registrada_por) VALUES (:codigo, 'Carrera',"
                                + " :siglas, 'MATRIZ', 'TECNOLOGICO_SUPERIOR', 'PRESENCIAL',"
                                + " CAST(:jornadas AS varchar(15)[]), :amplio, :especifico, null,"
                                + " :ciclos, 'VIGENTE', now(), 'vicerrectorado.demo')")
                .param("codigo", codigo)
                .param("siglas", siglas)
                .param("jornadas", jornadas)
                .param("amplio", amplio)
                .param("especifico", especifico)
                .param("ciclos", ciclos)
                .update();
    }

    private static int malla(JdbcClient jdbc, String codigo, String carrera) {
        return jdbc.sql(
                        "INSERT INTO academico.malla (codigo, carrera, estado, registrada_en,"
                                + " registrada_por) VALUES (:codigo, :carrera, 'VIGENTE', now(),"
                                + " 'vicerrectorado.demo')")
                .param("codigo", codigo)
                .param("carrera", carrera)
                .update();
    }

    private static int asignatura(
            JdbcClient jdbc,
            String codigo,
            String carrera,
            String malla,
            String horas,
            String palabras) {
        return jdbc.sql(
                        "INSERT INTO academico.asignatura (codigo, carrera, malla, nombre, ciclo,"
                                + " unidad_organizacion_curricular, horas_semanales_clase,"
                                + " horas_practicas_semanales, creditos, tronco_comun,"
                                + " campo_amplio, campo_especifico, palabras_clave, activa,"
                                + " registrada_en, registrada_por) VALUES (:codigo, :carrera,"
                                + " :malla, 'Asignatura', 1, 'BASICA', CAST(:horas AS numeric),"
                                + " null, null, false, '05', '054', CAST(:palabras AS"
                                + " varchar(300)[]), true, now(), 'vicerrectorado.demo')")
                .param("codigo", codigo)
                .param("carrera", carrera)
                .param("malla", malla)
                .param("horas", horas)
                .param("palabras", palabras)
                .update();
    }

    private static JdbcClient migrada(PostgreSQLContainer postgres) {
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();
        return JdbcClient.create(dataSource);
    }

    private static String nombre(JdbcClient jdbc, String codigo) {
        return jdbc.sql("SELECT nombre FROM academico.gestion_autorizada WHERE codigo = :codigo")
                .param("codigo", codigo)
                .query(String.class)
                .single();
    }

    private static int unidad(
            JdbcClient jdbc,
            String codigo,
            String tipo,
            String padre,
            String gestion,
            Boolean permanente) {
        return jdbc.sql(
                        "INSERT INTO academico.unidad_organica (codigo, nombre, tipo, padre,"
                                + " gestion_autorizada, comision_permanente, activa, registrada_en,"
                                + " registrada_por) VALUES (:codigo, 'Unidad', :tipo, :padre,"
                                + " :gestion, :permanente, true, now(), 'admin.demo')")
                .param("codigo", codigo)
                .param("tipo", tipo)
                .param("padre", padre)
                .param("gestion", gestion)
                .param("permanente", permanente)
                .update();
    }

    private static int periodo(
            JdbcClient jdbc, String codigo, String nombre, String fin, String estado) {
        return jdbc.sql(
                        "INSERT INTO academico.periodo_academico (codigo, nombre, inicio, fin,"
                                + " estado, registrado_en, registrado_por) VALUES (:codigo,"
                                + " :nombre, DATE '2026-09-01', CAST(:fin AS date), :estado,"
                                + " now(), 'vicerrectorado.demo')")
                .param("codigo", codigo)
                .param("nombre", nombre)
                .param("fin", fin)
                .param("estado", estado)
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
