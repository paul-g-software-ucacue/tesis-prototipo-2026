package ec.mdd.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.adaptadores.salida.RepositorioEventosRecibidosJdbc;
import ec.mdd.usuarios.infraestructura.UsuariosFlywayConfig;
import ec.mdd.usuarios.puertos.entrada.EventoRecibido;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Escenario «Entrega repetida» (FR-007, SC-004): el receptor registra cada {@code idEvento} una
 * sola vez contra PostgreSQL real, con entregas seguidas y simultáneas. Sin contexto de Spring:
 * cada inserción se confirma sola, como dos entregas en dos peticiones.
 */
class RepositorioEventosRecibidosJdbcIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");

    private static JdbcClient jdbc;
    private static RepositorioEventosRecibidosJdbc repositorio;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource dataSource =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new UsuariosFlywayConfig().flywayUsuarios(dataSource).migrate();
        jdbc = JdbcClient.create(dataSource);
        repositorio = new RepositorioEventosRecibidosJdbc(jdbc);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoElMismoEventoCincoVecesSeguidas_cuandoSeRegistra_entoncesQuedaUnaSolaFila() {
        EventoRecibido evento = evento();

        List<Boolean> resultados = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            resultados.add(repositorio.registrarSiEsNuevo(evento));
        }

        assertThat(resultados).containsExactly(true, false, false, false, false);
        assertThat(filas(evento.idEvento())).isEqualTo(1);
    }

    @Test
    void dadoDosEntregasSimultaneas_cuandoSeRegistran_entoncesSoloUnaLoRegistra() throws Exception {
        EventoRecibido evento = evento();
        CountDownLatch salida = new CountDownLatch(1);
        try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> entregas = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                entregas.add(
                        hilos.submit(
                                () -> {
                                    salida.await();
                                    return repositorio.registrarSiEsNuevo(evento);
                                }));
            }
            salida.countDown();

            List<Boolean> resultados = new ArrayList<>();
            for (Future<Boolean> entrega : entregas) {
                resultados.add(entrega.get());
            }
            assertThat(resultados).containsExactlyInAnyOrder(true, false);
        }
        assertThat(filas(evento.idEvento())).isEqualTo(1);
    }

    @Test
    void dadoUnEventoRepetido_cuandoSeRegistraOtraVez_entoncesConservaLaPrimeraRecepcion()
            throws InterruptedException {
        EventoRecibido evento = evento();
        repositorio.registrarSiEsNuevo(evento);
        Instant primera = recibidoEn(evento.idEvento());

        Thread.sleep(50);
        repositorio.registrarSiEsNuevo(evento);

        assertThat(recibidoEn(evento.idEvento())).isEqualTo(primera);
        assertThat(
                        jdbc.sql(
                                        "select tipo || '|' || origen from usuarios.evento_recibido"
                                                + " where id_evento = ?")
                                .param(evento.idEvento())
                                .query(String.class)
                                .single())
                .isEqualTo("generacion.EventoDePrueba|generacion");
    }

    private static EventoRecibido evento() {
        return new EventoRecibido(
                UUID.randomUUID(),
                "generacion.EventoDePrueba",
                "generacion",
                Instant.parse("2026-10-14T15:04:05Z"),
                Map.of("nota", "evento de prueba B00-H05"));
    }

    private static long filas(UUID idEvento) {
        return jdbc.sql("select count(*) from usuarios.evento_recibido where id_evento = ?")
                .param(idEvento)
                .query(Long.class)
                .single();
    }

    private static Instant recibidoEn(UUID idEvento) {
        return jdbc.sql("select recibido_en from usuarios.evento_recibido where id_evento = ?")
                .param(idEvento)
                .query(java.sql.Timestamp.class)
                .single()
                .toInstant();
    }
}
