package ec.mdd.validacionocs;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.validacionocs.adaptadores.salida.RepositorioConfiguracionJdbc;
import ec.mdd.validacionocs.dominio.AdvertenciaDeRemision;
import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.CodigoDeAdvertencia;
import ec.mdd.validacionocs.infraestructura.ValidacionocsFlywayConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Cada carga es una fila nueva, y la vigente es la última (FR-017, principio VII). */
class RepositorioConfiguracionIntegrationTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-02T15:00:00Z");
    private static final String QUIEN = "admin.demo";

    private static PostgreSQLContainer postgres;
    private static RepositorioConfiguracionJdbc repositorio;
    private static JdbcClient jdbc;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.7-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new ValidacionocsFlywayConfig().flywayValidacionocs(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioConfiguracionJdbc(jdbc);
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    private static String periodo() {
        return UUID.randomUUID().toString().substring(0, 20);
    }

    @Test
    void sinCargas_cuandoSeConsulta_entoncesVacio() {
        assertThat(repositorio.vigente(periodo())).isEmpty();
    }

    @Test
    void dadasDosCargas_cuandoSePideLaVigenteEnUnInstante_entoncesEsLaUltimaAnterior() {
        // F12-H02, research §5: la que regía cuando se creó el distributivo.
        String periodo = periodo();
        repositorio.guardar(periodo, "primera", QUIEN, MOMENTO);
        repositorio.guardar(periodo, "segunda", QUIEN, MOMENTO.plusSeconds(3600));

        assertThat(repositorio.vigenteEn(periodo, MOMENTO.minusSeconds(1))).isEmpty();
        assertThat(repositorio.vigenteEn(periodo, MOMENTO.plusSeconds(60)))
                .map(CargaGuardada::contenido)
                .contains("primera");
        assertThat(repositorio.vigenteEn(periodo, MOMENTO.plusSeconds(3600)))
                .map(CargaGuardada::contenido)
                .contains("segunda");
    }

    // F12-H03 (D-114): la advertencia de RN-23 se guarda con la carga y se lee igual.
    @Test
    void dadaUnaCargaConAdvertencia_cuandoSeConsulta_entoncesLaTraeTalCual() {
        String periodo = periodo();
        LocalDate hoy = LocalDate.of(2026, 11, 20);
        AdvertenciaDeRemision margen =
                new AdvertenciaDeRemision(
                        CodigoDeAdvertencia.RN23_MARGEN_INSUFICIENTE,
                        hoy.plusDays(43),
                        hoy.plusDays(60),
                        OptionalInt.of(30),
                        Optional.of(hoy.plusDays(30)),
                        OptionalInt.of(-13));
        repositorio.guardar(periodo, "uno", QUIEN, MOMENTO, Optional.of(margen));

        assertThat(repositorio.vigente(periodo).orElseThrow().advertencia()).contains(margen);
        assertThat(
                        repositorio
                                .vigenteEn(periodo, MOMENTO.plusSeconds(60))
                                .orElseThrow()
                                .advertencia())
                .contains(margen);
    }

    @Test
    void dadaUnaCargaSinNormativa_cuandoSeConsulta_entoncesTraeLaAdvertenciaSinLasCifras() {
        String periodo = periodo();
        AdvertenciaDeRemision sinNormativa =
                new AdvertenciaDeRemision(
                        CodigoDeAdvertencia.RN23_SIN_NORMATIVA,
                        LocalDate.of(2027, 1, 2),
                        LocalDate.of(2027, 6, 8),
                        OptionalInt.empty(),
                        Optional.empty(),
                        OptionalInt.empty());
        repositorio.guardar(periodo, "uno", QUIEN, MOMENTO, Optional.of(sinNormativa));
        repositorio.guardar(periodo, "dos", QUIEN, MOMENTO.plusSeconds(60));

        assertThat(repositorio.vigenteEn(periodo, MOMENTO).orElseThrow().advertencia())
                .contains(sinNormativa);
        assertThat(repositorio.vigente(periodo).orElseThrow().advertencia()).isEmpty();
    }

    @Test
    void dadaUnaCarga_cuandoSeConsulta_entoncesEsLaUnoConSuTextoTalCual() {
        String periodo = periodo();
        String texto = "# con su comentario\netapas: {}\n";

        repositorio.guardar(periodo, texto, QUIEN, MOMENTO);

        assertThat(repositorio.vigente(periodo))
                .contains(new CargaGuardada(1, texto, QUIEN, MOMENTO));
    }

    @Test
    void dadasDosCargas_cuandoSeConsulta_entoncesValeLaUltimaYSeConservanLasDos() {
        String periodo = periodo();
        repositorio.guardar(periodo, "primera", QUIEN, MOMENTO);
        repositorio.guardar(periodo, "segunda", QUIEN, MOMENTO.plusSeconds(60));

        CargaGuardada vigente = repositorio.vigente(periodo).orElseThrow();

        assertThat(vigente.numero()).isEqualTo(2);
        assertThat(vigente.contenido()).isEqualTo("segunda");
        assertThat(filas(periodo)).isEqualTo(2);
    }

    @Test
    void dadasCargasDeOtroPeriodo_cuandoSeConsulta_entoncesNoCuentan() {
        String periodo = periodo();
        repositorio.guardar(periodo(), "de otro", QUIEN, MOMENTO);
        repositorio.guardar(periodo, "mia", QUIEN, MOMENTO);

        CargaGuardada vigente = repositorio.vigente(periodo).orElseThrow();

        assertThat(vigente.numero()).isEqualTo(1);
        assertThat(vigente.contenido()).isEqualTo("mia");
    }

    @Test
    void dadasDosCargasALaVez_cuandoSeConsulta_entoncesQuedanLasDos() throws Exception {
        String periodo = periodo();
        CyclicBarrier salida = new CyclicBarrier(2);
        try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
            Future<?> una = hilos.submit(() -> cargar(salida, periodo, "una"));
            Future<?> otra = hilos.submit(() -> cargar(salida, periodo, "otra"));
            una.get(10, TimeUnit.SECONDS);
            otra.get(10, TimeUnit.SECONDS);
        }

        assertThat(filas(periodo)).isEqualTo(2);
        assertThat(repositorio.vigente(periodo).orElseThrow().numero()).isEqualTo(2);
    }

    private static Void cargar(CyclicBarrier salida, String periodo, String texto)
            throws Exception {
        salida.await(5, TimeUnit.SECONDS);
        repositorio.guardar(periodo, texto, QUIEN, MOMENTO);
        return null;
    }

    private static int filas(String periodo) {
        return jdbc.sql(
                        "select count(*) from validacionocs.configuracion_flujo where periodo ="
                                + " :periodo")
                .param("periodo", periodo)
                .query(Integer.class)
                .single();
    }
}
