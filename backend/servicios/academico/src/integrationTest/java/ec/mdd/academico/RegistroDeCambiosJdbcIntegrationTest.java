package ec.mdd.academico;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.academico.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.academico.infraestructura.AcademicoFlywayConfig;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Las cuatro acciones de F02-H01 quedan en academico.registro_cambio con su detalle (FR-008). */
class RegistroDeCambiosJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");

    private static JdbcClient jdbc;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new AcademicoFlywayConfig().flywayAcademico(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoLasCuatroAcciones_cuandoSeRegistran_entoncesQuedanConUsuarioMomentoYDetalle() {
        RegistroDeCambiosJdbc registro = new RegistroDeCambiosJdbc(jdbc);
        Instant momento = Instant.parse("2026-10-21T15:00:00Z");

        registro.registrar(
                new CambioRegistrado(
                        momento,
                        "vicerrectorado.demo",
                        Accion.PERIODO_REGISTRADO,
                        Map.of("codigo", "2026-2", "nombre", "Periodo 2026-2")));
        registro.registrar(
                new CambioRegistrado(
                        momento,
                        "vicerrectorado.demo",
                        Accion.PERIODO_CORREGIDO,
                        Map.of(
                                "codigo",
                                "2026-2",
                                "cambios",
                                List.of(
                                        Map.of(
                                                "campo", "fin",
                                                "anterior", "2027-02-28",
                                                "nuevo", "2027-03-15")))));
        registro.registrar(
                new CambioRegistrado(
                        momento,
                        "vicerrectorado.demo",
                        Accion.PERIODO_ABIERTO,
                        Map.of("codigo", "2026-2", "version", 2)));
        registro.registrar(
                new CambioRegistrado(
                        momento,
                        "vicerrectorado.demo",
                        Accion.PERIODO_ESTADO_CAMBIADO,
                        Map.of(
                                "codigo", "2026-2",
                                "anterior", "PLANIFICACION",
                                "nuevo", "EN_APROBACION")));

        List<String> filas =
                jdbc.sql(
                                "SELECT accion || ' ' || usuario || ' ' || (momento = :momento)"
                                        + " || ' ' || detalle::text FROM academico.registro_cambio"
                                        + " ORDER BY accion")
                        .param("momento", java.sql.Timestamp.from(momento))
                        .query(String.class)
                        .list();
        assertThat(filas)
                .containsExactly(
                        "PERIODO_ABIERTO vicerrectorado.demo true"
                                + " {\"codigo\": \"2026-2\", \"version\": 2}",
                        "PERIODO_CORREGIDO vicerrectorado.demo true {\"codigo\": \"2026-2\","
                                + " \"cambios\": [{\"campo\": \"fin\", \"nuevo\":"
                                + " \"2027-03-15\", \"anterior\": \"2027-02-28\"}]}",
                        "PERIODO_ESTADO_CAMBIADO vicerrectorado.demo true {\"nuevo\":"
                                + " \"EN_APROBACION\", \"codigo\": \"2026-2\", \"anterior\":"
                                + " \"PLANIFICACION\"}",
                        "PERIODO_REGISTRADO vicerrectorado.demo true {\"codigo\": \"2026-2\","
                                + " \"nombre\": \"Periodo 2026-2\"}");
    }
}
