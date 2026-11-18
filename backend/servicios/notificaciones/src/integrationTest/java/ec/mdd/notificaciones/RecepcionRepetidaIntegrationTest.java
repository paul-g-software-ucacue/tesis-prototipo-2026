package ec.mdd.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.notificaciones.adaptadores.salida.RepositorioEventosRecibidosJdbc;
import ec.mdd.notificaciones.adaptadores.salida.RepositorioNotificacionesJdbc;
import ec.mdd.notificaciones.aplicacion.RecibirNotificacionesServicio;
import ec.mdd.notificaciones.dominio.Destinatario;
import ec.mdd.notificaciones.dominio.DestinoDelMensaje;
import ec.mdd.notificaciones.infraestructura.NotificacionesFlywayConfig;
import ec.mdd.notificaciones.puertos.entrada.EventoRecibido;
import ec.mdd.notificaciones.puertos.salida.Destinatarios;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * Sin duplicados contra PostgreSQL real (F16-H01, US3, FR-012): el mismo evento, entregado otra
 * vez, después de leído o dos veces a la vez, deja una sola notificación por destinatario. Cada
 * recepción va en su transacción, como en el proceso.
 */
class RecepcionRepetidaIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-alpine");
    private static final String ROL = "identidad_prueba";
    private static final String CLAVE = "clave-de-prueba";

    private static JdbcClient jdbc;
    private static RecibirNotificacionesServicio servicio;
    private static TransactionTemplate transaccion;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        JdbcClient superusuario =
                JdbcClient.create(
                        new DriverManagerDataSource(
                                POSTGRES.getJdbcUrl(),
                                POSTGRES.getUsername(),
                                POSTGRES.getPassword()));
        superusuario.sql("create role " + ROL + " login password '" + CLAVE + "'").update();
        superusuario.sql("create schema notificaciones authorization " + ROL).update();
        DataSource proceso = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), ROL, CLAVE);
        new NotificacionesFlywayConfig().flywayNotificaciones(proceso).migrate();
        jdbc = JdbcClient.create(proceso);
        Destinatarios destinatarios =
                new Destinatarios() {
                    @Override
                    public List<Destinatario> de(DestinoDelMensaje destino, String periodo) {
                        return List.of(
                                new Destinatario("vicerrectorado.demo", Optional.empty()),
                                new Destinatario("vice.dos", Optional.empty()));
                    }

                    @Override
                    public Optional<String> nombre(String usuario) {
                        return Optional.empty();
                    }
                };
        servicio =
                new RecibirNotificacionesServicio(
                        new RepositorioEventosRecibidosJdbc(jdbc),
                        new RepositorioNotificacionesJdbc(jdbc),
                        destinatarios,
                        Clock.systemUTC());
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(proceso));
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadoElMismoEvento_cuandoLlegaDosVeces_entoncesUnaPorDestinatario() {
        EventoRecibido evento = evento();

        recibir(evento);
        recibir(evento);

        assertThat(filas(evento.idEvento())).isEqualTo(2);
    }

    // F16-H02: la notificación de firma pendiente, con su destino por identificación.
    @Test
    void dadaLaMismaSolicitudDeFirma_cuandoLlegaDosVeces_entoncesUnaPorDestinatario() {
        EventoRecibido evento = deFirma();

        recibir(evento);
        recibir(evento);

        assertThat(filas(evento.idEvento())).isEqualTo(2);
    }

    @Test
    void dadoElMismoEventoDespuesDeLeido_cuandoLlega_entoncesSigueUnaYSigueLeida() {
        EventoRecibido evento = evento();
        recibir(evento);
        jdbc.sql(
                        "UPDATE notificaciones.notificacion SET leida_en = now() WHERE id_evento ="
                                + " :id AND destinatario = 'vicerrectorado.demo'")
                .param("id", evento.idEvento())
                .update();

        recibir(evento);

        assertThat(filas(evento.idEvento())).isEqualTo(2);
        assertThat(
                        jdbc.sql(
                                        "SELECT count(*) FROM notificaciones.notificacion WHERE"
                                                + " id_evento = :id AND leida_en IS NOT NULL")
                                .param("id", evento.idEvento())
                                .query(Integer.class)
                                .single())
                .isOne();
    }

    @Test
    void dadoElMismoEvento_cuandoLlegaDosVecesALaVez_entoncesUnaPorDestinatario() throws Exception {
        EventoRecibido evento = evento();
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> entregas = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                entregas.add(
                        hilos.submit(
                                () -> {
                                    salida.await();
                                    recibir(evento);
                                    return null;
                                }));
            }
            salida.countDown();
            for (Future<?> entrega : entregas) {
                entrega.get(30, TimeUnit.SECONDS);
            }
        } finally {
            hilos.shutdownNow();
        }

        assertThat(filas(evento.idEvento())).isEqualTo(2);
    }

    private static void recibir(EventoRecibido evento) {
        transaccion.executeWithoutResult(estado -> servicio.recibir(evento));
    }

    private static int filas(UUID evento) {
        return jdbc.sql("SELECT count(*) FROM notificaciones.notificacion WHERE id_evento = :id")
                .param("id", evento)
                .query(Integer.class)
                .single();
    }

    private static EventoRecibido evento() {
        Map<String, Object> destino = new LinkedHashMap<>();
        destino.put("rol", "VICERRECTOR");
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("destino", destino);
        mensaje.put("titulo", "El distributivo llegó a «Revisión del Vicerrectorado»");
        mensaje.put("detalle", "Periodo 2026-I, versión 1.");
        mensaje.put("enlace", "/consolidado");
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("periodo", "2026-I");
        contenido.put("autor", "consolidacion.demo");
        contenido.put("momento", "2026-11-15T15:00:00Z");
        contenido.put("mensajes", List.of(mensaje));
        return new EventoRecibido(
                UUID.randomUUID(),
                "validacionocs.NotificacionesSolicitadas",
                "validacionocs",
                Instant.now(),
                contenido);
    }

    private static EventoRecibido deFirma() {
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("destino", Map.of("identificacion", "0109990028"));
        mensaje.put("titulo", "Tienes una firma pendiente: «Revisado por»");
        mensaje.put("detalle", "Periodo 2026-I, versión 1. «Elaborado por» la firmó Uno.");
        mensaje.put("enlace", "/firmas-pendientes");
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("periodo", "2026-I");
        contenido.put("autor", "docente.demo");
        contenido.put("momento", "2026-11-18T15:04:05Z");
        contenido.put("mensajes", List.of(mensaje));
        return new EventoRecibido(
                UUID.randomUUID(),
                "firma.NotificacionesSolicitadas",
                "firma",
                Instant.now(),
                contenido);
    }
}
