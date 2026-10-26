package ec.mdd.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.academico.adaptadores.salida.RepositorioMatriculasJdbc;
import ec.mdd.academico.dominio.Matricula;
import ec.mdd.academico.infraestructura.AcademicoFlywayConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Los matriculados se guardan y se leen igual, por periodo (F01-H04, V6), y la base exige su
 * periodo y su carrera. Sin V6 no hay tabla y fallan.
 */
class RepositorioMatriculasJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final Instant AHORA = Instant.parse("2026-10-26T15:00:00Z");
    private static final LocalDate CORTE = LocalDate.of(2026, 4, 30);

    private static JdbcClient jdbc;
    private static RepositorioMatriculasJdbc repositorio;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioMatriculasJdbc(jdbc);
        jdbc.sql(
                        "INSERT INTO academico.periodo_academico (codigo, nombre, inicio, fin, estado,"
                                + " registrado_en, registrado_por) VALUES ('2026-I', 'Primero', DATE"
                                + " '2026-04-06', DATE '2026-08-07', 'PLANIFICACION', now(), 'x'),"
                                + " ('2026-II', 'Segundo', DATE '2026-10-05', DATE '2027-02-05',"
                                + " 'PLANIFICACION', now(), 'x')")
                .update();
        jdbc.sql(
                        "INSERT INTO academico.unidad_organica (codigo, nombre, tipo, activa,"
                                + " registrada_en, registrada_por) VALUES ('MATRIZ', 'Sede',"
                                + " 'SEDE_MATRIZ', true, now(), 'x')")
                .update();
        jdbc.sql(
                        "INSERT INTO academico.carrera (codigo, nombre, siglas, sede, nivel_formacion,"
                                + " modalidad, jornadas, campo_amplio, campo_especifico,"
                                + " numero_ciclos, estado, registrada_en, registrada_por) VALUES"
                                + " ('C1', 'UNA', 'U', 'MATRIZ', 'TECNOLOGICO_SUPERIOR',"
                                + " 'PRESENCIAL', ARRAY['NOCTURNA']::varchar(15)[], '06', '061', 5, 'VIGENTE',"
                                + " now(), 'x')")
                .update();
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void guardaYLeeLosMatriculadosDeUnPeriodo() {
        Matricula primero = new Matricula("2026-I", "C1", 80, CORTE, "SIGA");
        Matricula segundo = new Matricula("2026-II", "C1", 85, CORTE, null);
        repositorio.registrar(primero, "datos.demo", AHORA);
        repositorio.registrar(segundo, "datos.demo", AHORA);
        repositorio.guardar(new Matricula("2026-I", "C1", 81, CORTE.plusDays(1), "Secretaría"));

        assertThat(repositorio.deLosPeriodos(List.of("2026-I")))
                .containsExactly(
                        new Matricula("2026-I", "C1", 81, CORTE.plusDays(1), "Secretaría"));
        assertThat(repositorio.deLosPeriodos(List.of("2026-I", "2026-II"))).hasSize(2);
        assertThat(repositorio.deLosPeriodos(List.of())).isEmpty();
    }

    @Test
    void laBaseExigeElPeriodoYLaCarrera() {
        assertThatThrownBy(
                        () ->
                                repositorio.registrar(
                                        new Matricula("2099-I", "C1", 1, CORTE, null), "x", AHORA))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                repositorio.registrar(
                                        new Matricula("2026-I", "NO-EXISTE", 1, CORTE, null),
                                        "x",
                                        AHORA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
