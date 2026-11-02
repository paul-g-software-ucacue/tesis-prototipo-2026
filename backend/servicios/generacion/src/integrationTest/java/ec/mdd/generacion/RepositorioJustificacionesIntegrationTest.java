package ec.mdd.generacion;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.generacion.adaptadores.salida.RepositorioDistributivosJdbc;
import ec.mdd.generacion.adaptadores.salida.RepositorioJustificacionesJdbc;
import ec.mdd.generacion.dominio.JustificacionDeInconsistencia;
import ec.mdd.generacion.dominio.TipoDeInconsistencia;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.infraestructura.GeneracionFlywayConfig;
import java.time.Instant;
import java.util.List;
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

/** Las justificaciones solo se insertan, y vale la última (principio VII). */
class RepositorioJustificacionesIntegrationTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-02T15:00:00Z");
    private static final String QUIEN = "consolidacion.demo";
    private static final String DOCENTE = "0109990010";

    private static PostgreSQLContainer postgres;
    private static RepositorioDistributivosJdbc distributivos;
    private static RepositorioJustificacionesJdbc repositorio;
    private static JdbcClient jdbc;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.6-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new GeneracionFlywayConfig().flywayGeneracion(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        distributivos = new RepositorioDistributivosJdbc(jdbc);
        repositorio = new RepositorioJustificacionesJdbc(jdbc);
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadaUnaJustificacion_cuandoSeGuarda_entoncesSeLeeIgual() {
        UUID version = version();
        JustificacionDeInconsistencia justificacion = justificacion("El primer motivo", MOMENTO);

        repositorio.guardar(version, justificacion);

        assertThat(repositorio.vigentes(version)).containsExactly(justificacion);
    }

    @Test
    void dadasDosDelMismoObjeto_cuandoSeLee_entoncesValeLaUltimaYSeConservanLasDos() {
        UUID version = version();
        repositorio.guardar(version, justificacion("El primer motivo", MOMENTO));
        repositorio.guardar(version, justificacion("El segundo motivo", MOMENTO.plusSeconds(60)));

        assertThat(repositorio.vigentes(version))
                .extracting(JustificacionDeInconsistencia::texto)
                .containsExactly("El segundo motivo");
        assertThat(filas(version)).isEqualTo(2);
    }

    @Test
    void dadosTiposUObjetosDistintos_cuandoSeLee_entoncesCadaUnoTieneLaSuya() {
        UUID version = version();
        repositorio.guardar(version, justificacion("El primer motivo", MOMENTO));
        repositorio.guardar(
                version,
                new JustificacionDeInconsistencia(
                        TipoDeInconsistencia.ACTIVIDADES_OBLIGATORIAS,
                        DOCENTE,
                        "Otro tipo, el mismo docente",
                        QUIEN,
                        MOMENTO));
        repositorio.guardar(
                version,
                new JustificacionDeInconsistencia(
                        TipoDeInconsistencia.PROPORCION,
                        "0109990020",
                        "El mismo tipo, otro docente",
                        QUIEN,
                        MOMENTO));

        assertThat(repositorio.vigentes(version)).hasSize(3);
    }

    @Test
    void dadaUnaJustificacionDeOtraVersion_cuandoSeLee_entoncesNoCuenta() {
        UUID una = version();
        UUID otra = version();
        repositorio.guardar(una, justificacion("El primer motivo", MOMENTO));

        assertThat(repositorio.vigentes(otra)).isEmpty();
    }

    @Test
    void dadosDosAlaVez_cuandoSeGuardan_entoncesQuedanLasDosYUnaEsLaVigente() throws Exception {
        UUID version = version();
        CyclicBarrier barrera = new CyclicBarrier(2);
        try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
            List<Future<Void>> trabajos =
                    List.of(
                            hilos.submit(() -> guardarJuntos(barrera, version, "Motivo de uno")),
                            hilos.submit(() -> guardarJuntos(barrera, version, "Motivo de otro")));
            for (Future<Void> trabajo : trabajos) {
                trabajo.get(30, TimeUnit.SECONDS);
            }
        }

        assertThat(filas(version)).isEqualTo(2);
        assertThat(repositorio.vigentes(version)).hasSize(1);
    }

    private static Void guardarJuntos(CyclicBarrier barrera, UUID version, String texto)
            throws Exception {
        barrera.await(10, TimeUnit.SECONDS);
        repositorio.guardar(version, justificacion(texto, MOMENTO));
        return null;
    }

    private static long filas(UUID version) {
        return jdbc.sql(
                        "SELECT count(*) FROM generacion.justificacion_inconsistencia"
                                + " WHERE version_id = :version")
                .param("version", version)
                .query(Long.class)
                .single();
    }

    private static UUID version() {
        VersionDistributivo version =
                VersionDistributivo.primera(UUID.randomUUID(), List.of("TDS"));
        distributivos.crear(
                "P-" + UUID.randomUUID().toString().substring(0, 8), version, QUIEN, MOMENTO);
        return version.id();
    }

    private static JustificacionDeInconsistencia justificacion(String texto, Instant en) {
        return new JustificacionDeInconsistencia(
                TipoDeInconsistencia.PROPORCION, DOCENTE, texto, QUIEN, en);
    }
}
