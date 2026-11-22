package ec.mdd.generacion;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.generacion.adaptadores.salida.RepositorioContenidoCongeladoJdbc;
import ec.mdd.generacion.adaptadores.salida.RepositorioDistributivosJdbc;
import ec.mdd.generacion.adaptadores.salida.TransaccionSpring;
import ec.mdd.generacion.dominio.ActividadCongelada;
import ec.mdd.generacion.dominio.AsignaturaCongelada;
import ec.mdd.generacion.dominio.CargaCongelada;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.ContenidoCongelado;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.DocenteDeLaVersion;
import ec.mdd.generacion.dominio.TipoDeIdentificacion;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.infraestructura.GeneracionFlywayConfig;
import ec.mdd.generacion.puertos.entrada.MotivoDeVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
 * El contenido congelado en PostgreSQL real (F20-H01, D-115): se guarda una sola vez, vuelve igual
 * y la versión publicada se sigue leyendo cuando la reforma tiene otra versión vigente.
 */
class ContenidoCongeladoIntegrationTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-21T15:00:00Z");
    private static final String HASH = "a".repeat(64);

    private static PostgreSQLContainer postgres;
    private static RepositorioDistributivosJdbc distributivos;
    private static RepositorioContenidoCongeladoJdbc repositorio;
    private static TransaccionSpring transaccion;

    @BeforeAll
    static void levantar() {
        postgres = new PostgreSQLContainer("postgres:18.7-alpine");
        postgres.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new GeneracionFlywayConfig().flywayGeneracion(dataSource).migrate();
        JdbcClient jdbc = JdbcClient.create(dataSource);
        distributivos = new RepositorioDistributivosJdbc(jdbc);
        repositorio = new RepositorioContenidoCongeladoJdbc(jdbc);
        transaccion =
                new TransaccionSpring(
                        new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    @AfterAll
    static void apagar() {
        postgres.stop();
    }

    @Test
    void dadoUnContenido_cuandoSeGuardaDosVeces_entoncesQuedaElPrimeroYVuelveIgual() {
        String periodo = periodo();
        VersionDistributivo version = congelada(periodo);
        ContenidoCongelado contenido = contenido(periodo, 1, "8");

        boolean primera = repositorio.guardar(version.id(), contenido, MOMENTO, false);
        boolean segunda =
                repositorio.guardar(version.id(), contenido(periodo, 1, "6"), MOMENTO, true);

        assertThat(primera).isTrue();
        assertThat(segunda).isFalse();
        assertThat(repositorio.hash(periodo, 1)).contains(HASH);
        // F20-H02 (D-117): cada docente trae la dedicación y las horas de su propia carga.
        assertThat(repositorio.docentes(periodo, 1))
                .containsExactly(
                        DocenteDeLaVersion.de(
                                repositorio.carga(periodo, 1, "0109990020").orElseThrow()),
                        DocenteDeLaVersion.de(
                                repositorio.carga(periodo, 1, "0109990010").orElseThrow()));
        DocenteDeLaVersion ana = repositorio.docentes(periodo, 1).get(1);
        assertThat(ana.identificacion()).isEqualTo("0109990010");
        assertThat(ana.dedicacion()).contains(Dedicacion.TC);
        assertThat(ana.horas().clase()).isEqualByComparingTo("8");
        assertThat(repositorio.docentes(periodo, 1).get(0).dedicacion()).contains(Dedicacion.MT);
        CargaCongelada leida = repositorio.carga(periodo, 1, "0109990010").orElseThrow();
        assertThat(leida).isEqualTo(carga("0109990010", "Pérez Ana", "8", Dedicacion.TC, 40));
        assertThat(leida.horas().clase()).isEqualByComparingTo("8");
        assertThat(repositorio.carga(periodo, 1, "0109990099")).isEmpty();
        assertThat(repositorio.carga(periodo, 2, "0109990010")).isEmpty();
        // F19-H01 (D-119): el tipo de identificación vuelve igual, y todas las cargas de una vez
        // son las mismas que de a una, por nombre.
        assertThat(leida.tipoIdentificacion()).contains(TipoDeIdentificacion.CEDULA);
        assertThat(repositorio.cargas(periodo, 1))
                .containsExactly(
                        repositorio.carga(periodo, 1, "0109990020").orElseThrow(),
                        repositorio.carga(periodo, 1, "0109990010").orElseThrow());
        assertThat(repositorio.cargas(periodo, 2)).isEmpty();
    }

    @Test
    void dadoUnDocenteSinDedicacion_cuandoSeGuarda_entoncesVuelveSinDedicacion() {
        String periodo = periodo();
        VersionDistributivo version = congelada(periodo);
        CargaCongelada sinDedicacion = carga("0109990010", "Pérez Ana", "8", null, null);

        repositorio.guardar(
                version.id(),
                new ContenidoCongelado(periodo, 1, HASH, List.of(sinDedicacion)),
                MOMENTO,
                true);

        CargaCongelada leida = repositorio.carga(periodo, 1, "0109990010").orElseThrow();
        assertThat(leida.dedicacion()).isEmpty();
        assertThat(leida.horasContratadas()).isEmpty();
        // Sin la clave tipoIdentificacion en el JSON: el tipo queda vacío (D-119).
        assertThat(leida.tipoIdentificacion()).isEmpty();
        assertThat(repositorio.docentes(periodo, 1))
                .singleElement()
                .satisfies(
                        docente -> {
                            assertThat(docente.dedicacion()).isEmpty();
                            assertThat(docente.horas()).isEqualTo(leida.horas());
                        });
    }

    @Test
    void dadaUnaReforma_cuandoSePideLaVersionPublicada_entoncesSeLeeAunqueNoSeaLaVigente() {
        String periodo = periodo();
        VersionDistributivo publicada = congelada(periodo);
        repositorio.guardar(publicada.id(), contenido(periodo, 1, "8"), MOMENTO, false);

        transaccion.ejecutar(
                () ->
                        distributivos.crearVersionNueva(
                                publicada.id(),
                                MotivoDeVersion.REFORMA,
                                "vicerrectorado.demo",
                                MOMENTO));

        assertThat(distributivos.vigenteDe(periodo).orElseThrow().numero()).isEqualTo(2);
        assertThat(distributivos.version(periodo, 1).orElseThrow().id()).isEqualTo(publicada.id());
        assertThat(distributivos.version(periodo, 3)).isEmpty();
        assertThat(repositorio.docentes(periodo, 1)).hasSize(2);
    }

    private static VersionDistributivo congelada(String periodo) {
        VersionDistributivo version =
                VersionDistributivo.primera(UUID.randomUUID(), List.of("TDS"));
        distributivos.crear(periodo, version, "consolidacion.demo", MOMENTO);
        distributivos.congelar(version.id());
        return distributivos.vigenteDe(periodo).orElseThrow();
    }

    private static ContenidoCongelado contenido(String periodo, int numero, String clase) {
        return new ContenidoCongelado(
                periodo,
                numero,
                HASH,
                List.of(
                        carga("0109990010", "Pérez Ana", clase, Dedicacion.TC, 40),
                        carga("0109990020", "Andrade Luis", "0", Dedicacion.MT, 20)));
    }

    private static CargaCongelada carga(
            String docente, String nombre, String clase, Dedicacion dedicacion, Integer horas) {
        BigDecimal deClase = new BigDecimal(clase);
        List<AsignaturaCongelada> asignaturas =
                deClase.signum() == 0
                        ? List.of()
                        : List.of(
                                new AsignaturaCongelada(
                                        "BD-301",
                                        "Bases de datos",
                                        "A",
                                        "TDS",
                                        "Software",
                                        deClase));
        return new CargaCongelada(
                docente,
                nombre,
                dedicacion == null ? null : TipoDeIdentificacion.CEDULA,
                dedicacion,
                horas,
                asignaturas,
                List.of(
                        new ActividadCongelada(
                                "141-a",
                                "Proyecto",
                                Componente.INVESTIGACION,
                                "TDS",
                                "INV-01",
                                "Suelos andinos",
                                new BigDecimal("2.50"))),
                new CargaHoraria(
                        deClase,
                        BigDecimal.ZERO,
                        new BigDecimal("2.50"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO));
    }

    private static String periodo() {
        return "P" + UUID.randomUUID().toString().substring(0, 8);
    }
}
