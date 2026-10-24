package ec.mdd.academico;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.academico.adaptadores.salida.RepositorioCarrerasJdbc;
import ec.mdd.academico.adaptadores.salida.RepositorioEstructuraJdbc;
import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.EstadoDeCarrera;
import ec.mdd.academico.dominio.EstadoDeMalla;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.infraestructura.AcademicoFlywayConfig;
import java.math.BigDecimal;
import java.time.Instant;
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

/**
 * Las carreras, mallas y asignaturas se guardan y se leen igual, campo por campo (F03-H02, T008).
 */
class RepositorioCarrerasJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");

    private static DataSource dataSource;
    private static JdbcClient jdbc;
    private static RepositorioCarrerasJdbc repositorio;

    private static final Carrera DS =
            Carrera.nueva(
                    "550613A01-P-0301",
                    "DESARROLLO DE SOFTWARE",
                    "DS",
                    "MATRIZ",
                    NivelDeFormacion.TECNOLOGICO_SUPERIOR,
                    Modalidad.PRESENCIAL,
                    List.of(Jornada.NOCTURNA, Jornada.MATUTINA),
                    new CampoConocimiento("06", "061"),
                    "0613",
                    5);
    private static final Malla DS_2019 = Malla.nueva("DS-2019", "550613A01-P-0301");
    private static final Asignatura MD =
            Asignatura.nueva(
                    "DS-B-I-MD-101",
                    "550613A01-P-0301",
                    "DS-2019",
                    "MATEMÁTICA DISCRETA",
                    1,
                    UnidadDeOrganizacionCurricular.BASICA,
                    new BigDecimal("3"),
                    BigDecimal.ZERO,
                    new BigDecimal("2.5"),
                    true,
                    new CampoConocimiento("05", "054"),
                    List.of("matemática", "lógica", "conjuntos"));

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioCarrerasJdbc(jdbc);
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
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @BeforeEach
    void vaciar() {
        jdbc.sql("DELETE FROM academico.asignatura").update();
        jdbc.sql("DELETE FROM academico.malla").update();
        jdbc.sql("DELETE FROM academico.carrera").update();
    }

    @Test
    void dadaUnaCarreraUnaMallaYUnaAsignatura_cuandoSeGuardan_entoncesSeLeenIguales() {
        repositorio.registrar(DS, "vicerrectorado.demo", AHORA);
        repositorio.registrar(DS_2019, "vicerrectorado.demo", AHORA);
        repositorio.registrar(MD, "vicerrectorado.demo", AHORA);

        assertThat(repositorio.carreras()).containsExactly(DS);
        assertThat(repositorio.mallas()).containsExactly(DS_2019);
        assertThat(repositorio.asignaturas())
                .singleElement()
                .satisfies(
                        leida -> {
                            assertThat(leida.palabrasClave())
                                    .containsExactly("matemática", "lógica", "conjuntos");
                            assertThat(leida.horasSemanalesClase()).isEqualByComparingTo("3");
                            assertThat(leida.creditos()).isEqualByComparingTo("2.5");
                            assertThat(leida.conEstado(true))
                                    .usingRecursiveComparison()
                                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                                    .isEqualTo(MD);
                        });
        assertThat(repositorio.camposAmplios()).hasSize(11);
        assertThat(repositorio.camposAmplios().get(6).nombre())
                .isEqualTo("Tecnologías de la información y la comunicación");
    }

    @Test
    void dadosLosGuardados_cuandoCambian_entoncesSeLeenConSusDatosNuevos() {
        repositorio.registrar(DS, "vicerrectorado.demo", AHORA);
        repositorio.registrar(DS_2019, "vicerrectorado.demo", AHORA);
        repositorio.registrar(MD, "vicerrectorado.demo", AHORA);

        Carrera corregida =
                DS.corregida(
                                "SOFTWARE",
                                "SW",
                                "MATRIZ",
                                NivelDeFormacion.TECNICO_SUPERIOR,
                                Modalidad.DUAL,
                                List.of(Jornada.VESPERTINA),
                                new CampoConocimiento("06", "061"),
                                null,
                                6)
                        .carrera()
                        .conEstado(EstadoDeCarrera.EN_CIERRE);
        repositorio.guardar(corregida);
        repositorio.guardar(DS_2019.conEstado(EstadoDeMalla.DEROGADA));
        Asignatura inactiva = MD.conEstado(false);
        repositorio.guardar(inactiva);

        assertThat(repositorio.carreras()).containsExactly(corregida);
        assertThat(repositorio.mallas().getFirst().estado()).isEqualTo(EstadoDeMalla.DEROGADA);
        assertThat(repositorio.asignaturas().getFirst().activa()).isFalse();
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
