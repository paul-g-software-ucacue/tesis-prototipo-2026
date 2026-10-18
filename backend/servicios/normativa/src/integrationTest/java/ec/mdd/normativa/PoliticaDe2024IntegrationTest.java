package ec.mdd.normativa;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.normativa.infraestructura.NormativaFlywayConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Después de migrar, la versión 1 es la Política SENESCYT 2024: sus 22 filas coinciden, valor por
 * valor, con la carga 7 (SC-001), y tiene el tipo de IST, las reglas y el registro de la historia.
 * La carga 7 está copiada en {@code politica-2024.csv}, así que un cambio en la migración o en el
 * archivo hace fallar esta prueba.
 */
class PoliticaDe2024IntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");

    private static final String FILAS =
            "select f.aplica_a, f.perfil, f.dedicacion_total, f.horas_clase, f.horas_clase_minimo,"
                    + " f.horas_clase_maximo, f.horas_otras_docencia, f.horas_gestion,"
                    + " f.horas_inv_vinc_maximo, f.severidad, f.referencia, f.observacion"
                    + " from normativa.fila_carga_horaria f"
                    + " join normativa.version_normativa v on v.id = f.version_id"
                    + " where v.numero = 1 order by f.orden";

    private static JdbcClient jdbc;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new NormativaFlywayConfig().flywayNormativa(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoLaMigracion_cuandoSeLeenLasVersiones_entoncesHayUnaSolaLaUnoConElTipo2()
            throws Exception {
        List<Map<String, Object>> versiones =
                jdbc.sql(
                                "select numero, codigo, origen, vigente_desde::text as vigencia,"
                                        + " tipo_ist from normativa.version_normativa")
                        .query()
                        .listOfRows();

        assertThat(versiones)
                .singleElement()
                .isEqualTo(
                        Map.of(
                                "numero", 1,
                                "codigo", "SENESCYT-2024-03",
                                "origen", "Política SENESCYT 2024",
                                "vigencia", "2024-03-11",
                                "tipo_ist", "T2"));
    }

    @Test
    void dadoLaMigracion_cuandoSeComparaConLaCarga7_entoncesLas22FilasCoincidenValorPorValor()
            throws IOException {
        List<List<String>> esperadas = cargaSiete();
        List<List<String>> enLaBase =
                jdbc.sql(FILAS)
                        .query(
                                (fila, numero) -> {
                                    List<String> valores = new ArrayList<>();
                                    for (int columna = 1; columna <= 12; columna++) {
                                        valores.add(Objects.toString(fila.getObject(columna), ""));
                                    }
                                    return valores;
                                })
                        .list();

        assertThat(esperadas).hasSize(22);
        assertThat(enLaBase).containsExactlyElementsOf(esperadas);
    }

    @Test
    void dadoLaMigracion_cuandoSeLeenLasReglas_entoncesSonLasSieteConSus29Parametros() {
        Map<String, String> severidades = new LinkedHashMap<>();
        jdbc.sql(
                        "select r.identificador, coalesce(r.severidad, '—') from normativa.regla r"
                                + " order by 1")
                .query((fila, numero) -> severidades.put(fila.getString(1), fila.getString(2)))
                .list();
        Map<String, BigDecimal> parametros = new LinkedHashMap<>();
        jdbc.sql("select regla || '.' || nombre, valor from normativa.parametro_regla")
                .query((fila, numero) -> parametros.put(fila.getString(1), fila.getBigDecimal(2)))
                .list();

        assertThat(severidades)
                .containsExactly(
                        Map.entry("RN-01", "—"),
                        Map.entry("RN-02", "DURA"),
                        Map.entry("RN-05", "BLANDA"),
                        Map.entry("RN-06", "—"),
                        Map.entry("RN-09", "BLANDA"),
                        Map.entry("RN-10", "BLANDA"),
                        Map.entry("RN-13", "DURA"));
        assertThat(parametros).hasSize(29);
        assertThat(parametros)
                .containsEntry("RN-01.dedicacion_tiempo_parcial_maxima", new BigDecimal("19.00"))
                .containsEntry("RN-02.horas_maximas_docente", new BigDecimal("40.00"))
                .containsEntry("RN-05.porcentaje_clase", new BigDecimal("60.00"))
                .containsEntry("RN-06.t2_hasta", new BigDecimal("600.00"))
                .containsEntry("RN-09.estudiantes_minimos", new BigDecimal("500.00"))
                .containsEntry("RN-10.t2_maximo", new BigDecimal("11.00"))
                .containsEntry("RN-13.porcentaje_nomina", new BigDecimal("10.00"));
    }

    @Test
    void dadoLaMigracion_cuandoSeLeeElRegistro_entoncesLaVersionUnoQuedoCreadaPorElSistema() {
        Map<String, Object> registro =
                jdbc.sql(
                                "select usuario, accion, detalle ->> 'codigo' as codigo"
                                        + " from normativa.registro_cambio")
                        .query()
                        .singleRow();

        assertThat(registro)
                .containsEntry("usuario", "sistema")
                .containsEntry("accion", "VERSION_CREADA")
                .containsEntry("codigo", "SENESCYT-2024-03");
    }

    // Las columnas de la carga 7 que guarda la fila, en el orden de FILAS.
    private static List<List<String>> cargaSiete() throws IOException {
        List<List<String>> filas = new ArrayList<>();
        try (InputStream archivo =
                        PoliticaDe2024IntegrationTest.class.getResourceAsStream(
                                "/politica-2024.csv");
                BufferedReader lector =
                        new BufferedReader(
                                new InputStreamReader(
                                        Objects.requireNonNull(archivo), StandardCharsets.UTF_8))) {
            lector.readLine();
            for (String linea = lector.readLine(); linea != null; linea = lector.readLine()) {
                List<String> c = columnas(linea);
                // codigo_version, vigencia, tipo_ist, perfil, dedicación, clase, mínimo, máximo,
                // otras de docencia, gestión, investigación y vinculación, tipo de regla,
                // referencia y observación.
                filas.add(
                        List.of(
                                c.get(2), c.get(3), c.get(4), c.get(5), c.get(6), c.get(7),
                                c.get(8), c.get(9), c.get(10), c.get(11), c.get(12), c.get(13)));
            }
        }
        return filas;
    }

    // Separa una línea CSV con campos entre comillas dobles, como los de la carga 7.
    private static List<String> columnas(String linea) {
        List<String> columnas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (char caracter : linea.toCharArray()) {
            if (caracter == '"') {
                entreComillas = !entreComillas;
            } else if (caracter == ',' && !entreComillas) {
                columnas.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(caracter);
            }
        }
        columnas.add(actual.toString());
        return columnas;
    }
}
