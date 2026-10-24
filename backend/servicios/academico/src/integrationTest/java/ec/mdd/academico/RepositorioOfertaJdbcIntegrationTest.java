package ec.mdd.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import ec.mdd.academico.adaptadores.salida.RepositorioCarrerasJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioEstructuraJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioOfertaJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioPeriodosJdbc;
import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.AsignaturaParalelo;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.EstadoDeAsignaturaParalelo;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.infraestructura.AcademicoFlywayConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** La oferta del periodo se guarda y se lee igual, campo por campo (F03-H03, T007). */
class RepositorioOfertaJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");

    private static DataSource dataSource;
    private static JdbcClient jdbc;
    private static RepositorioOfertaJdbc repositorio;

    private static final AsignaturaParalelo SIN_DATOS =
            AsignaturaParalelo.nueva(
                    "2026-I",
                    "A1",
                    "A_NOCTURNA",
                    Jornada.NOCTURNA,
                    Modalidad.PRESENCIAL,
                    null,
                    28,
                    null);
    private static final AsignaturaParalelo CON_DATOS =
            AsignaturaParalelo.nueva(
                    "2026-I",
                    "A1",
                    "B_NOCTURNA",
                    Jornada.NOCTURNA,
                    Modalidad.DUAL,
                    "MATRIZ",
                    0,
                    new BigDecimal("2.5"));

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioOfertaJdbc(jdbc);
        new RepositorioEstructuraJdbc(jdbc)
                .registrar(
                        UnidadOrganica.nueva(
                                "MATRIZ",
                                "Sede matriz",
                                TipoDeUnidad.SEDE_MATRIZ,
                                null,
                                null,
                                null),
                        "admin.demo",
                        AHORA);
        RepositorioCarrerasJdbc carreras = new RepositorioCarrerasJdbc(jdbc);
        carreras.registrar(
                Carrera.nueva(
                        "C1",
                        "DESARROLLO DE SOFTWARE",
                        "DS",
                        "MATRIZ",
                        NivelDeFormacion.TECNOLOGICO_SUPERIOR,
                        Modalidad.PRESENCIAL,
                        List.of(Jornada.NOCTURNA),
                        new CampoConocimiento("06", "061"),
                        null,
                        5),
                "vicerrectorado.demo",
                AHORA);
        carreras.registrar(Malla.nueva("M1", "C1"), "vicerrectorado.demo", AHORA);
        for (String codigo : List.of("A1", "A2")) {
            carreras.registrar(
                    Asignatura.nueva(
                            codigo,
                            "C1",
                            "M1",
                            "ASIGNATURA",
                            1,
                            UnidadDeOrganizacionCurricular.BASICA,
                            new BigDecimal("4"),
                            null,
                            null,
                            false,
                            new CampoConocimiento("06", "061"),
                            List.of("uno", "dos")),
                    "vicerrectorado.demo",
                    AHORA);
        }
        RepositorioPeriodosJdbc periodos = new RepositorioPeriodosJdbc(jdbc);
        for (String codigo : List.of("2026-I", "2026-II")) {
            periodos.registrar(
                    PeriodoAcademico.nuevo(
                            codigo,
                            "Periodo " + codigo,
                            LocalDate.of(2026, 4, 6),
                            LocalDate.of(2026, 8, 7)),
                    "vicerrectorado.demo",
                    AHORA);
        }
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @BeforeEach
    void vaciar() {
        jdbc.sql("DELETE FROM academico.asignatura_paralelo").update();
    }

    @Test
    void dadasDosAsignaturasParalelo_cuandoSeGuardan_entoncesSeLeenIgualesConYSinHorasNiSede() {
        repositorio.registrar(CON_DATOS, "vicerrectorado.demo", AHORA);
        repositorio.registrar(SIN_DATOS, "vicerrectorado.demo", AHORA);

        List<AsignaturaParalelo> leidas = repositorio.delPeriodo("2026-I");

        assertThat(leidas).hasSize(2);
        assertThat(leidas.getFirst()).isEqualTo(SIN_DATOS);
        assertThat(leidas.get(1))
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(CON_DATOS);
    }

    @Test
    void dadaLaOfertaDeDosPeriodos_cuandoSeLeeUno_entoncesTraeSoloLaSuyaPorAsignaturaYParalelo() {
        repositorio.registrar(
                AsignaturaParalelo.nueva(
                        "2026-II",
                        "A1",
                        "A_NOCTURNA",
                        Jornada.NOCTURNA,
                        Modalidad.PRESENCIAL,
                        null,
                        9,
                        null),
                "vicerrectorado.demo",
                AHORA);
        repositorio.registrar(
                AsignaturaParalelo.nueva(
                        "2026-I",
                        "A2",
                        "A_NOCTURNA",
                        Jornada.NOCTURNA,
                        Modalidad.PRESENCIAL,
                        null,
                        9,
                        null),
                "vicerrectorado.demo",
                AHORA);
        repositorio.registrar(CON_DATOS, "vicerrectorado.demo", AHORA);
        repositorio.registrar(SIN_DATOS, "vicerrectorado.demo", AHORA);

        assertThat(repositorio.delPeriodo("2026-I"))
                .extracting(AsignaturaParalelo::asignatura, AsignaturaParalelo::paralelo)
                .containsExactly(
                        tuple("A1", "A_NOCTURNA"),
                        tuple("A1", "B_NOCTURNA"),
                        tuple("A2", "A_NOCTURNA"));
        assertThat(repositorio.delPeriodo("2026-II")).hasSize(1);
    }

    @Test
    void dadaUnaGuardada_cuandoCambiaCadaDatoYSuEstado_entoncesSeLeeConLosNuevos() {
        repositorio.registrar(SIN_DATOS, "vicerrectorado.demo", AHORA);
        AsignaturaParalelo corregida =
                SIN_DATOS
                        .corregida(
                                Jornada.NOCTURNA,
                                Modalidad.HIBRIDA,
                                "MATRIZ",
                                31,
                                new BigDecimal("3"))
                        .asignaturaParalelo()
                        .conEstado(EstadoDeAsignaturaParalelo.RETIRADA);

        repositorio.guardar(corregida);

        assertThat(repositorio.delPeriodo("2026-I"))
                .singleElement()
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(corregida);
        repositorio.guardar(SIN_DATOS);
        assertThat(repositorio.delPeriodo("2026-I")).containsExactly(SIN_DATOS);
    }

    @Test
    void dadoElCandado_cuandoOtraTransaccionLoPide_entoncesEspera() throws Exception {
        TransactionTemplate transaccion =
                new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        CompletableFuture<Void> liberar = new CompletableFuture<>();
        CompletableFuture<Void> primera =
                CompletableFuture.runAsync(
                        () ->
                                transaccion.executeWithoutResult(
                                        estado -> {
                                            repositorio.bloquear();
                                            liberar.join();
                                        }));
        Thread.sleep(500);

        CompletableFuture<Void> segunda =
                CompletableFuture.runAsync(
                        () -> transaccion.executeWithoutResult(estado -> repositorio.bloquear()));

        assertThat(esperaMasDe(segunda, 500)).isTrue();
        liberar.complete(null);
        primera.get(5, TimeUnit.SECONDS);
        segunda.get(5, TimeUnit.SECONDS);
    }

    private static boolean esperaMasDe(CompletableFuture<Void> tarea, long milisegundos)
            throws Exception {
        try {
            tarea.get(milisegundos, TimeUnit.MILLISECONDS);
            return false;
        } catch (TimeoutException esperada) {
            return true;
        }
    }
}
