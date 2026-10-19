package ec.mdd.roles;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.roles.infraestructura.RolesFlywayConfig;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Después de migrar, cada rol de D-16 tiene exactamente los permisos de su fila en
 * specs/011-roles-permisos/data-model.md (SC-002, D-65), con el cambio de D-66, y cada
 * funcionalidad sus tipos. La tabla esperada está escrita aquí a mano a propósito: si alguien
 * cambia la migración, esta prueba falla.
 */
class MatrizD16IntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");

    private static final Map<String, List<String>> MATRIZ =
            Map.of(
                    "ADMINISTRADOR",
                    List.of(
                            "BITACORA:LECTURA",
                            "CARGAS:LECTURA",
                            "CATALOGOS:ESCRITURA",
                            "CATALOGOS:LECTURA",
                            "CONSOLIDACION:LECTURA",
                            "DISTRIBUTIVO:LECTURA",
                            "DOCENTES:LECTURA",
                            "FLUJO_FIRMAS:ESCRITURA",
                            "FLUJO_FIRMAS:LECTURA",
                            "OFERTA:LECTURA",
                            "PARAMETROS:ESCRITURA",
                            "PARAMETROS:LECTURA",
                            "PROYECTOS:LECTURA",
                            "REPORTES:LECTURA",
                            "RESOLUCION_OCS:LECTURA",
                            "USUARIOS_ROLES:ESCRITURA",
                            "USUARIOS_ROLES:LECTURA"),
                    "OPERADOR_DATOS",
                    List.of(
                            "CARGAS:ESCRITURA",
                            "CARGAS:LECTURA",
                            "DOCENTES:ESCRITURA",
                            "DOCENTES:LECTURA"),
                    "COORDINADOR_CARRERA",
                    List.of(
                            "DISTRIBUTIVO:LECTURA:PROPIO",
                            "DOCENTES:LECTURA",
                            "OFERTA:APROBACION:PROPIO",
                            "OFERTA:ESCRITURA:PROPIO",
                            "OFERTA:LECTURA:PROPIO"),
                    "GESTOR_UNIDAD",
                    List.of(
                            "PROYECTOS:APROBACION:PROPIO",
                            "PROYECTOS:ESCRITURA:PROPIO",
                            "PROYECTOS:LECTURA:PROPIO"),
                    "RESPONSABLE_CONSOLIDACION",
                    List.of(
                            "CONSOLIDACION:APROBACION",
                            "CONSOLIDACION:ESCRITURA",
                            "CONSOLIDACION:LECTURA",
                            "DISTRIBUTIVO:FIRMA",
                            "DISTRIBUTIVO:LECTURA",
                            "OFERTA:LECTURA",
                            "PROYECTOS:LECTURA"),
                    "VICERRECTOR",
                    List.of(
                            "BITACORA:LECTURA",
                            "DISTRIBUTIVO:APROBACION",
                            "DISTRIBUTIVO:ESCRITURA",
                            "DISTRIBUTIVO:FIRMA",
                            "DISTRIBUTIVO:LECTURA",
                            // D-66: lee la normativa que elige para su periodo (F04-H01).
                            "PARAMETROS:LECTURA",
                            "REPORTES:LECTURA"),
                    "RECTOR",
                    List.of(
                            "DISTRIBUTIVO:APROBACION",
                            "DISTRIBUTIVO:FIRMA",
                            "DISTRIBUTIVO:LECTURA",
                            "REPORTES:LECTURA"),
                    "MIEMBRO_OCS",
                    List.of("DISTRIBUTIVO:LECTURA", "REPORTES:LECTURA", "VOTACION_OCS:APROBACION"),
                    "SECRETARIA_GENERAL",
                    List.of(
                            "DISTRIBUTIVO:LECTURA",
                            "PUBLICACION:APROBACION",
                            "REPORTES:LECTURA",
                            "RESOLUCION_OCS:ESCRITURA",
                            "RESOLUCION_OCS:LECTURA"),
                    "DOCENTE",
                    List.of("MI_DISTRIBUTIVO:ESCRITURA:PROPIO", "MI_DISTRIBUTIVO:LECTURA:PROPIO"));

    private static final Map<String, String> TIPOS =
            Map.ofEntries(
                    Map.entry("USUARIOS_ROLES", "ESCRITURA,LECTURA"),
                    Map.entry("PARAMETROS", "ESCRITURA,LECTURA"),
                    Map.entry("CATALOGOS", "ESCRITURA,LECTURA"),
                    Map.entry("FLUJO_FIRMAS", "ESCRITURA,LECTURA"),
                    Map.entry("BITACORA", "LECTURA"),
                    Map.entry("CARGAS", "ESCRITURA,LECTURA"),
                    Map.entry("DOCENTES", "ESCRITURA,LECTURA"),
                    Map.entry("OFERTA", "APROBACION,ESCRITURA,LECTURA"),
                    Map.entry("PROYECTOS", "APROBACION,ESCRITURA,LECTURA"),
                    Map.entry("CONSOLIDACION", "APROBACION,ESCRITURA,LECTURA"),
                    Map.entry("DISTRIBUTIVO", "APROBACION,ESCRITURA,FIRMA,LECTURA"),
                    Map.entry("REPORTES", "LECTURA"),
                    Map.entry("VOTACION_OCS", "APROBACION"),
                    Map.entry("RESOLUCION_OCS", "ESCRITURA,LECTURA"),
                    Map.entry("PUBLICACION", "APROBACION"),
                    Map.entry("MI_DISTRIBUTIVO", "ESCRITURA,LECTURA"));

    private static JdbcClient jdbc;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new RolesFlywayConfig().flywayRoles(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoLaMigracion_cuandoSeLeenLosRolesDeD16_entoncesCadaUnoTieneExactamenteSuFila() {
        Map<String, List<String>> enLaBase = new TreeMap<>();
        jdbc.sql(
                        "select r.identificador, rp.funcionalidad || ':' || rp.tipo"
                                + " || case when rp.alcance = 'PROPIO' then ':PROPIO' else '' end"
                                + " from roles.rol r join roles.rol_permiso rp on rp.rol_id = r.id"
                                + " order by 1, 2")
                .query((fila, numero) -> Map.entry(fila.getString(1), fila.getString(2)))
                .list()
                .forEach(
                        par ->
                                enLaBase.computeIfAbsent(
                                                par.getKey(), clave -> new java.util.ArrayList<>())
                                        .add(par.getValue()));

        assertThat(enLaBase).containsExactlyInAnyOrderEntriesOf(MATRIZ);
    }

    @Test
    void dadoLaMigracion_cuandoSeLeenLosRoles_entoncesSonLos10DeD16YTodosPredefinidos() {
        List<String> predefinidos =
                jdbc.sql("select identificador from roles.rol where predefinido order by 1")
                        .query(String.class)
                        .list();

        assertThat(predefinidos).containsExactlyInAnyOrderElementsOf(MATRIZ.keySet());
        assertThat(jdbc.sql("select count(*) from roles.rol").query(Long.class).single())
                .isEqualTo(10L);
    }

    @Test
    void dadoLaMigracion_cuandoSeLeenLasFuncionalidades_entoncesSonLas16ConSusTipos() {
        Map<String, String> enLaBase =
                jdbc
                        .sql(
                                "select funcionalidad, string_agg(tipo, ',' order by tipo)"
                                        + " from roles.funcionalidad_tipo group by funcionalidad")
                        .query((fila, numero) -> Map.entry(fila.getString(1), fila.getString(2)))
                        .list()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(enLaBase).containsExactlyInAnyOrderEntriesOf(TIPOS);
        assertThat(
                        jdbc.sql("select codigo from roles.funcionalidad order by orden")
                                .query(String.class)
                                .list())
                .first()
                .isEqualTo("USUARIOS_ROLES");
    }
}
