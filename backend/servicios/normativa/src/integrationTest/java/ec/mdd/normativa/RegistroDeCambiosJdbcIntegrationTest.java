package ec.mdd.normativa;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.normativa.adaptadores.salida.RegistroDeCambiosJdbc;
import ec.mdd.normativa.infraestructura.NormativaFlywayConfig;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa.Accion;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Las tres acciones de F04-H02 quedan en normativa.registro_cambio con su detalle (FR-009). */
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
        new NormativaFlywayConfig().flywayNormativa(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoLasTresAcciones_cuandoSeRegistran_entoncesQuedanConUsuarioMomentoYDetalle() {
        RegistroDeCambiosJdbc registro = new RegistroDeCambiosJdbc(jdbc);
        Instant momento = Instant.parse("2026-10-18T15:00:00Z");
        Map<String, Object> cambio = new LinkedHashMap<>();
        cambio.put("campo", "tablaCargaHoraria[DOCENTE_MT/TODOS].horasOtrasDocencia");
        cambio.put("anterior", null);
        cambio.put("nuevo", "8");

        registro.registrar(
                new CambioDeNormativa(
                        momento,
                        "vicerrectorado.demo",
                        Accion.VERSION_CREADA,
                        Map.of("numero", 2, "codigo", "SENESCYT-2025-01", "copiadaDe", 1)));
        registro.registrar(
                new CambioDeNormativa(
                        momento,
                        "vicerrectorado.demo",
                        Accion.VERSION_EDITADA,
                        Map.of("numero", 2, "cambios", List.of(cambio))));
        registro.registrar(
                new CambioDeNormativa(
                        momento,
                        "vicerrectorado.demo",
                        Accion.VERSION_ASOCIADA,
                        Map.of("numero", 2, "periodo", "2025-1")));

        List<String> filas =
                jdbc.sql(
                                "SELECT accion || ' ' || usuario || ' ' || (momento = :momento)"
                                        + " || ' ' || detalle::text FROM normativa.registro_cambio"
                                        + " WHERE usuario <> 'sistema' ORDER BY accion")
                        .param("momento", java.sql.Timestamp.from(momento))
                        .query(String.class)
                        .list();
        assertThat(filas)
                .containsExactly(
                        "VERSION_ASOCIADA vicerrectorado.demo true {\"numero\": 2, \"periodo\":"
                                + " \"2025-1\"}",
                        "VERSION_CREADA vicerrectorado.demo true {\"codigo\": \"SENESCYT-2025-01\","
                                + " \"numero\": 2, \"copiadaDe\": 1}",
                        "VERSION_EDITADA vicerrectorado.demo true {\"numero\": 2, \"cambios\":"
                                + " [{\"campo\":"
                                + " \"tablaCargaHoraria[DOCENTE_MT/TODOS].horasOtrasDocencia\","
                                + " \"nuevo\": \"8\", \"anterior\": null}]}");
    }
}
