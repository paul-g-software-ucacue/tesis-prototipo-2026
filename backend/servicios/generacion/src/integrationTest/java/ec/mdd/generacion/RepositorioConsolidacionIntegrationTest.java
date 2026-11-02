package ec.mdd.generacion;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.generacion.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.generacion.adaptadores.salida.RepositorioConsolidacionJdbc;
import ec.mdd.generacion.adaptadores.salida.RepositorioDistributivosJdbc;
import ec.mdd.generacion.dominio.Consolidacion;
import ec.mdd.generacion.dominio.InformeSustentado;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.infraestructura.GeneracionFlywayConfig;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

/** Consolidaciones e informes solo se insertan, y vale el último (principio VII). */
class RepositorioConsolidacionIntegrationTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-02T15:00:00Z");
    private static final String QUIEN = "consolidacion.demo";

    private static PostgreSQLContainer postgres;
    private static RepositorioDistributivosJdbc distributivos;
    private static RepositorioConsolidacionJdbc repositorio;
    private static RegistroDeCambiosJdbc registro;
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
        repositorio = new RepositorioConsolidacionJdbc(jdbc);
        registro = new RegistroDeCambiosJdbc(jdbc);
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void sinConsolidar_cuandoSeLee_entoncesVacio() {
        UUID version = version();

        assertThat(repositorio.ultimaConsolidacion(version)).isEmpty();
        assertThat(repositorio.ultimoInforme(version)).isEmpty();
    }

    @Test
    void dadasDosConsolidaciones_cuandoSeLee_entoncesValeLaUltimaYSeConservanLasDos() {
        UUID version = version();
        repositorio.guardarConsolidacion(version, new Consolidacion(QUIEN, MOMENTO));
        Consolidacion ultima = new Consolidacion("otra.persona", MOMENTO.plusSeconds(60));
        repositorio.guardarConsolidacion(version, ultima);

        assertThat(repositorio.ultimaConsolidacion(version)).contains(ultima);
        assertThat(filas("consolidacion", version)).isEqualTo(2);
    }

    @Test
    void dadasDosALaVez_cuandoSeGuardan_entoncesQuedanLasDosYHayUnaVigente() throws Exception {
        UUID version = version();
        CyclicBarrier barrera = new CyclicBarrier(2);
        try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
            List<Future<Void>> trabajos =
                    List.of(
                            hilos.submit(() -> consolidarJuntos(barrera, version, "una.persona")),
                            hilos.submit(() -> consolidarJuntos(barrera, version, "otra.persona")));
            for (Future<Void> trabajo : trabajos) {
                trabajo.get(30, TimeUnit.SECONDS);
            }
        }

        assertThat(filas("consolidacion", version)).isEqualTo(2);
        assertThat(repositorio.ultimaConsolidacion(version)).isPresent();
    }

    @Test
    void dadosDosInformes_cuandoSeLee_entoncesValeElUltimoYSeConservanLosDos() {
        UUID version = version();
        repositorio.guardarInforme(version, informe("a.pdf", MOMENTO));
        InformeSustentado ultimo = informe("b.pdf", MOMENTO.plusSeconds(60));
        repositorio.guardarInforme(version, ultimo);

        assertThat(repositorio.ultimoInforme(version)).contains(ultimo);
        assertThat(filas("informe_sustentado", version)).isEqualTo(2);
    }

    @Test
    void unInformeDeOtraVersion_cuandoSeLee_entoncesNoCuenta() {
        UUID una = version();
        repositorio.guardarInforme(una, informe("a.pdf", MOMENTO));

        assertThat(repositorio.ultimoInforme(version())).isEmpty();
    }

    @Test
    void dadosCambiosDeAsignaciones_cuandoSeConsulta_entoncesDevuelveElUltimo() {
        String periodo = periodo();
        registrar(periodo, CambioDeDistributivo.Accion.PROPUESTA_GUARDADA, MOMENTO);
        registrar(periodo, CambioDeDistributivo.Accion.PROPUESTA_QUITADA, MOMENTO.plusSeconds(1));
        registrar(periodo, CambioDeDistributivo.Accion.ACTIVIDAD_GUARDADA, MOMENTO.plusSeconds(2));
        registrar(periodo, CambioDeDistributivo.Accion.ACTIVIDAD_QUITADA, MOMENTO.plusSeconds(3));

        assertThat(repositorio.ultimoCambioDeAsignaciones(periodo))
                .contains(MOMENTO.plusSeconds(3));
    }

    @Test
    void lasDemasAcciones_cuandoSeConsulta_entoncesNoCuentan() {
        String periodo = periodo();
        registrar(periodo, CambioDeDistributivo.Accion.PROPUESTA_GUARDADA, MOMENTO);
        registrar(
                periodo,
                CambioDeDistributivo.Accion.INCONSISTENCIA_JUSTIFICADA,
                MOMENTO.plusSeconds(1));
        registrar(periodo, CambioDeDistributivo.Accion.INFORME_ADJUNTADO, MOMENTO.plusSeconds(2));
        registrar(
                periodo,
                CambioDeDistributivo.Accion.DISTRIBUTIVO_CONSOLIDADO,
                MOMENTO.plusSeconds(3));

        assertThat(repositorio.ultimoCambioDeAsignaciones(periodo)).contains(MOMENTO);
    }

    @Test
    void losCambiosDeOtroPeriodo_cuandoSeConsulta_entoncesNoCuentan() {
        registrar(periodo(), CambioDeDistributivo.Accion.PROPUESTA_GUARDADA, MOMENTO);

        assertThat(repositorio.ultimoCambioDeAsignaciones(periodo())).isEmpty();
    }

    private static Void consolidarJuntos(CyclicBarrier barrera, UUID version, String quien)
            throws Exception {
        barrera.await(10, TimeUnit.SECONDS);
        repositorio.guardarConsolidacion(version, new Consolidacion(quien, MOMENTO));
        return null;
    }

    private static void registrar(
            String periodo, CambioDeDistributivo.Accion accion, Instant momento) {
        registro.registrar(
                new CambioDeDistributivo(momento, QUIEN, accion, Map.of("periodo", periodo)));
    }

    private static long filas(String tabla, UUID version) {
        return jdbc.sql("SELECT count(*) FROM generacion." + tabla + " WHERE version_id = :version")
                .param("version", version)
                .query(Long.class)
                .single();
    }

    private static String periodo() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static UUID version() {
        VersionDistributivo version =
                VersionDistributivo.primera(UUID.randomUUID(), List.of("TDS"));
        distributivos.crear(periodo(), version, QUIEN, MOMENTO);
        return version.id();
    }

    private static InformeSustentado informe(String nombre, Instant en) {
        return new InformeSustentado(
                nombre, 100, "a".repeat(64), UUID.randomUUID().toString(), QUIEN, en);
    }
}
