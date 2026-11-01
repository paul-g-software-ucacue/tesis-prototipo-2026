package ec.mdd.normativa;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.normativa.adaptadores.salida.RepositorioActividadesJdbc;
import ec.mdd.normativa.adaptadores.salida.RepositorioVersionesJdbc;
import ec.mdd.normativa.adaptadores.salida.TransaccionSpring;
import ec.mdd.normativa.dominio.ActividadDelCatalogo;
import ec.mdd.normativa.dominio.Componente;
import ec.mdd.normativa.dominio.DatosDeVersion;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.infraestructura.NormativaFlywayConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * El catálogo de actividades de los arts. 140 a 143 del RCE (RF-21, RN-22, D-79). La lista esperada
 * está escrita aquí, aparte de la migración, para que un literal mal copiado no pase la prueba.
 */
class CatalogoDeActividadesIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");

    private static final String CARGAR =
            "SELECT a.codigo, a.componente, array_to_string(a.tipos_de_proyecto, ',') AS tipos,"
                    + " array_to_string(a.cargos, ',') AS cargos, a.nombre"
                    + " FROM normativa.actividad_catalogo a"
                    + " JOIN normativa.version_normativa v ON v.id = a.version_id"
                    + " WHERE v.numero = :numero ORDER BY a.orden";

    private static final String PRACTICAS = "PRACTICAS_PREPROFESIONALES,SERVICIO_COMUNITARIO";
    private static final String VINCULACION = "VINCULACION," + PRACTICAS;

    private static RepositorioVersionesJdbc repositorio;
    private static TransaccionSpring transaccion;
    private static JdbcClient jdbc;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new NormativaFlywayConfig().flywayNormativa(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioVersionesJdbc(jdbc);
        transaccion =
                new TransaccionSpring(
                        new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    private record Fila(
            String codigo, String componente, String tipos, String cargos, String nombre) {}

    private static List<Fila> catalogo(int numero) {
        return jdbc.sql(CARGAR)
                .param("numero", numero)
                .query(
                        (fila, indice) ->
                                new Fila(
                                        fila.getString("codigo"),
                                        fila.getString("componente"),
                                        fila.getString("tipos"),
                                        fila.getString("cargos"),
                                        fila.getString("nombre")))
                .list();
    }

    private static List<String> literales(int articulo, char desde, char hasta, char sin) {
        List<String> codigos = new ArrayList<>();
        for (char literal = desde; literal <= hasta; literal++) {
            if (literal != sin) {
                codigos.add(articulo + "-" + literal);
            }
        }
        return codigos;
    }

    private static Map<String, String> tiposEsperados() {
        Map<String, String> esperados = new TreeMap<>();
        esperados.put("140-f", PRACTICAS);
        literales(141, 'a', 'i', ' ').forEach(codigo -> esperados.put(codigo, "INVESTIGACION"));
        literales(142, 'a', 'i', ' ').forEach(codigo -> esperados.put(codigo, VINCULACION));
        return esperados;
    }

    private static Map<String, String> cargosEsperados() {
        Map<String, String> esperados = new TreeMap<>();
        esperados.put("143-a", "RECTOR,VICERRECTOR");
        esperados.put("143-b", "COORDINADOR_ACADEMICO_INSTITUCIONAL");
        esperados.put("143-c", "COORDINADOR_SEDE");
        esperados.put("143-d", "COORDINADOR_CARRERA");
        esperados.put("143-e", "G07_GESTOR_INVESTIGACION");
        esperados.put("143-f", "G06_GESTOR_VINCULACION");
        esperados.put("143-g", "G08_GESTOR_IDIOMAS");
        esperados.put("143-h", "G09_GESTOR_FORMACION_INTEGRAL");
        return esperados;
    }

    @Test
    void dadaLaVersionUno_cuandoSeLee_entoncesTieneLas47ActividadesEnElOrdenDeLaNorma() {
        List<String> esperadas = new ArrayList<>();
        esperadas.addAll(literales(140, 'b', 'o', ' '));
        esperadas.addAll(literales(141, 'a', 'i', ' '));
        esperadas.addAll(literales(142, 'a', 'i', ' '));
        esperadas.addAll(literales(143, 'a', 'p', 'i'));

        assertThat(catalogo(1)).extracting(Fila::codigo).containsExactlyElementsOf(esperadas);
        assertThat(esperadas).hasSize(47);
    }

    @Test
    void dadoElCatalogo_cuandoSeCuentaPorComponente_entoncesSonLas14Las9Las9YLas15() {
        Map<String, Long> cuenta = new TreeMap<>();
        catalogo(1).forEach(fila -> cuenta.merge(fila.componente(), 1L, Long::sum));

        assertThat(cuenta)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "OTRAS_DOCENCIA", 14L,
                                "INVESTIGACION", 9L,
                                "VINCULACION", 9L,
                                "GESTION", 15L));
    }

    @Test
    void dadoElCatalogo_cuandoSeBuscaLoExcluido_entoncesNoEstaNiHablaDeComisiones() {
        assertThat(catalogo(1))
                .extracting(Fila::codigo)
                .doesNotContain("140-a", "140-p", "141-j", "142-j", "143-i", "143-q");
        assertThat(catalogo(1))
                .extracting(fila -> fila.nombre().toLowerCase(java.util.Locale.ROOT))
                .noneMatch(nombre -> nombre.contains("comisi") || nombre.startsWith("las demás"));
    }

    @Test
    void dadoElCatalogo_cuandoSeMiraLoQueExige_entoncesCoincideConLaListaEscritaAparte() {
        Map<String, String> tipos = new TreeMap<>();
        Map<String, String> cargos = new TreeMap<>();
        for (Fila fila : catalogo(1)) {
            if (!fila.tipos().isEmpty()) {
                tipos.put(fila.codigo(), fila.tipos());
            }
            if (!fila.cargos().isEmpty()) {
                cargos.put(fila.codigo(), fila.cargos());
            }
        }

        assertThat(tipos).isEqualTo(tiposEsperados());
        assertThat(cargos).isEqualTo(cargosEsperados());
    }

    @Test
    void dadosLiteralesDeLaNorma_cuandoSeLeen_entoncesTraenSuTextoCompleto() {
        Map<String, String> nombres = new TreeMap<>();
        catalogo(1).forEach(fila -> nombres.put(fila.codigo(), fila.nombre()));

        assertThat(nombres)
                .containsEntry(
                        "140-b",
                        "Planificar y actualizar contenidos de clases, seminarios, talleres, entre otros")
                .containsEntry(
                        "140-g",
                        "Preparar, elaborar, aplicar, calificar y registrar exámenes, trabajos, proyectos y prácticas")
                .containsEntry("143-d", "Coordinar carreras o programas")
                .containsEntry(
                        "143-k",
                        "Organizar o dirigir eventos académicos nacionales o internacionales");
        assertThat(nombres.values())
                .allMatch(nombre -> !nombre.endsWith(";") && !nombre.endsWith(","));
    }

    @Test
    void dadoElCatalogo_cuandoSeMigra_entoncesQuedaRegistradoComoEdicionDelSistema() {
        assertThat(
                        jdbc.sql(
                                        "SELECT count(*) FROM normativa.registro_cambio"
                                                + " WHERE usuario = 'sistema'"
                                                + " AND detalle::text LIKE '%actividades%'")
                                .query(Long.class)
                                .single())
                .isEqualTo(1L);
    }

    @Test
    void dadoElRepositorio_cuandoLee_entoncesTraeCadaActividadConLoQueExige() {
        List<ActividadDelCatalogo> actividades =
                new RepositorioActividadesJdbc(jdbc).deLaVersion(1);

        assertThat(actividades).hasSize(47);
        assertThat(actividades.getFirst())
                .isEqualTo(
                        new ActividadDelCatalogo(
                                "140-b",
                                "Planificar y actualizar contenidos de clases, seminarios,"
                                        + " talleres, entre otros",
                                Componente.OTRAS_DOCENCIA,
                                List.of(),
                                List.of()));
        assertThat(actividades.get(4).tiposDeProyecto())
                .containsExactly("PRACTICAS_PREPROFESIONALES", "SERVICIO_COMUNITARIO");
        assertThat(actividades.get(32).cargos()).containsExactly("RECTOR", "VICERRECTOR");
    }

    @Test
    void dadaUnaVersionNueva_cuandoSeGuarda_entoncesCopiaElCatalogo() {
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        VersionNormativa copia =
                transaccion.ejecutar(
                        () -> {
                            VersionNormativa nueva =
                                    VersionNormativa.copiaDe(
                                                    repositorio.buscar(1).orElseThrow(),
                                                    repositorio.siguienteNumero(),
                                                    new DatosDeVersion(
                                                            "CATALOGO-1",
                                                            "Prueba del catálogo",
                                                            LocalDate.of(2025, 1, 15)),
                                                    ahora)
                                            .version();
                            repositorio.guardar(nueva, "vicerrectorado.demo", ahora);
                            return nueva;
                        });

        assertThat(catalogo(copia.numero())).isEqualTo(catalogo(1));
    }
}
