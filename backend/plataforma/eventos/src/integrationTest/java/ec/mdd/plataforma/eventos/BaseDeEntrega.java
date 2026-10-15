package ec.mdd.plataforma.eventos;

import static org.awaitility.Awaitility.await;

import ec.mdd.plataforma.pruebas.ReceptorSimulado;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Lo común a las pruebas de integración de la entrega: PostgreSQL 18.6 de Testcontainers, el
 * registro en {@code eventos_prueba}, el receptor simulado y consultas al registro.
 */
abstract class BaseDeEntrega {

    // Uno para todas las clases de la suite: cada prueba que lo detiene lo vuelve a levantar. Se
    // cierra con la JVM.
    static final ReceptorSimulado RECEPTOR = new ReceptorSimulado();

    @Autowired JdbcClient jdbc;

    @Autowired AplicacionDePruebaEventos.ServicioDePrueba servicio;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.modulith.events.jdbc.schema", () -> "eventos_prueba");
        registro.add("mdd.eventos.destinos.usuarios", () -> RECEPTOR.urlEventos().toString());
        registro.add("mdd.eventos.token-proceso", () -> RECEPTOR.urlToken().toString());
        registro.add("mdd.eventos.proceso", () -> "prueba");
        registro.add("mdd.eventos.clave", () -> "clave-de-prueba");
        registro.add("mdd.eventos.plazo", () -> "PT2S");
    }

    @BeforeEach
    void prepararTabla() {
        jdbc.sql(
                        "create table if not exists eventos_prueba.fila_prueba"
                                + " (id_evento uuid primary key)")
                .update();
    }

    String estado(UUID idEvento) {
        List<String> estados =
                jdbc.sql(
                                "select status from eventos_prueba.event_publication"
                                        + " where serialized_event like ?")
                        .param("%" + idEvento + "%")
                        .query(String.class)
                        .list();
        return estados.isEmpty() ? "SIN_PUBLICACION" : estados.getFirst();
    }

    void esperarEstado(UUID idEvento, String esperado, Duration plazo) {
        await().atMost(plazo)
                .pollInterval(Duration.ofMillis(200))
                .until(() -> esperado.equals(estado(idEvento)));
    }

    long filas(UUID idEvento) {
        return jdbc.sql("select count(*) from eventos_prueba.fila_prueba where id_evento = ?")
                .param(idEvento)
                .query(Long.class)
                .single();
    }

    long recibidos(UUID idEvento) {
        return RECEPTOR.eventosRecibidos().stream()
                .filter(peticion -> peticion.cuerpo().contains(idEvento.toString()))
                .count();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Contenedores {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18.6-alpine");
        }
    }
}
