package ec.mdd.cargas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.cargas.adaptadores.salida.HistorialDeCargasJdbc;
import ec.mdd.cargas.dominio.EntradaDeHistorial;
import ec.mdd.cargas.dominio.MotivoDeRechazo;
import ec.mdd.cargas.dominio.ResultadoDeHistorial;
import ec.mdd.cargas.infraestructura.CargasFlywayConfig;
import java.time.Instant;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * El historial de cargas con PostgreSQL real (F01-H04, RF-10): guarda y lee cada resultado, en
 * orden, encuentra la primera aplicación de un archivo y la base rechaza lo que no cumple sus
 * reglas. Sin V2 no hay tabla y todas fallan.
 */
class HistorialDeCargasJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final String HASH = "a".repeat(64);
    private static final Instant PRIMERA = Instant.parse("2026-10-26T10:00:00Z");

    private static JdbcClient jdbc;
    private static HistorialDeCargasJdbc historial;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new CargasFlywayConfig().flywayCargas(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        historial = new HistorialDeCargasJdbc(jdbc);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    private static EntradaDeHistorial entrada(
            String carga, Instant momento, ResultadoDeHistorial resultado, MotivoDeRechazo motivo) {
        return new EntradaDeHistorial(
                carga,
                "datos.demo",
                momento,
                "02_carreras.csv",
                HASH,
                resultado,
                resultado == ResultadoDeHistorial.APLICADA ? 7 : 0,
                0,
                1,
                Optional.ofNullable(motivo));
    }

    @Test
    void guardaYLeeCadaResultado_delMasRecienteAlMasAntiguo() {
        EntradaDeHistorial aplicada =
                entrada("carreras", PRIMERA, ResultadoDeHistorial.APLICADA, null);
        EntradaDeHistorial repetida =
                entrada("carreras", PRIMERA.plusSeconds(60), ResultadoDeHistorial.REPETIDA, null);
        EntradaDeHistorial rechazada =
                entrada(
                        "carreras",
                        PRIMERA.plusSeconds(120),
                        ResultadoDeHistorial.RECHAZADA,
                        MotivoDeRechazo.RECHAZADA_POR_EL_DUENO);
        historial.registrar(aplicada);
        historial.registrar(repetida);
        historial.registrar(rechazada);
        historial.registrar(entrada("docentes", PRIMERA, ResultadoDeHistorial.REPETIDA, null));

        assertThat(historial.deLaCarga("carreras")).containsExactly(rechazada, repetida, aplicada);
        assertThat(historial.aplicada("carreras", HASH)).contains(aplicada);
        assertThat(historial.aplicada("docentes", HASH)).isEmpty();
        assertThat(historial.aplicada("carreras", "b".repeat(64))).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "hash = 'XYZ'",
                "resultado = 'BORRADA'",
                "nuevos = -1",
                "motivo = NULL",
                "carga = 'Carreras'"
            })
    void laBaseRechazaLoQueNoCumpleSusReglas(String cambio) {
        historial.registrar(
                entrada(
                        "estructura_organica",
                        PRIMERA,
                        ResultadoDeHistorial.RECHAZADA,
                        MotivoDeRechazo.CON_ERRORES));

        assertThatThrownBy(
                        () ->
                                jdbc.sql(
                                                "UPDATE cargas.historial_carga SET "
                                                        + cambio
                                                        + " WHERE carga = 'estructura_organica'")
                                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unaAplicadaNoTieneMotivo() {
        assertThatThrownBy(
                        () ->
                                jdbc.sql(
                                                "INSERT INTO cargas.historial_carga (carga, usuario,"
                                                        + " momento, archivo, hash, resultado,"
                                                        + " nuevos, cambiados, bajas_sin_aplicar,"
                                                        + " motivo) VALUES ('carreras', 'x', now(),"
                                                        + " 'a.csv', :hash, 'APLICADA', 0, 0, 0,"
                                                        + " 'CON_ERRORES')")
                                        .param("hash", HASH)
                                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
