package ec.mdd.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.notificaciones.infraestructura.NotificacionesFlywayConfig;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * La V2 de Notificaciones contra PostgreSQL real (F16-H01, data-model.md): una notificación por
 * evento, destinatario y carrera, y su contenido no cambia; solo {@code leida_en}, una vez (FR-009,
 * FR-012). Migra con un rol que no es superusuario y es dueño del esquema, como el rol {@code
 * identidad} del perfil consolidado.
 */
class BandejaEsquemaIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.7-alpine");
    private static final String ROL = "identidad_prueba";
    private static final String CLAVE = "clave-de-prueba";

    private static JdbcClient comoProceso;
    private static JdbcClient comoSuperusuario;

    @BeforeAll
    static void migrar() {
        POSTGRES.start();
        DataSource superusuario =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        comoSuperusuario = JdbcClient.create(superusuario);
        comoSuperusuario.sql("create role " + ROL + " login password '" + CLAVE + "'").update();
        comoSuperusuario.sql("create schema notificaciones authorization " + ROL).update();
        DataSource proceso = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), ROL, CLAVE);
        new NotificacionesFlywayConfig().flywayNotificaciones(proceso).migrate();
        comoProceso = JdbcClient.create(proceso);
    }

    @AfterAll
    static void detener() {
        POSTGRES.stop();
    }

    @Test
    void dadaUnaNotificacion_cuandoSeMarcaLeida_entoncesSoloCambiaLeidaEnYUnaSolaVez() {
        UUID id = insertar(evento(), "vicerrectorado.demo", null);

        int marcadas =
                comoProceso
                        .sql(
                                "update notificaciones.notificacion set leida_en = now() where id = :id")
                        .param("id", id)
                        .update();

        assertThat(marcadas).isEqualTo(1);
        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "update notificaciones.notificacion set leida_en ="
                                                        + " now() where id = :id")
                                        .param("id", id)
                                        .update())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void
            dadaUnaNotificacion_cuandoSeIntentaCambiarSuContenido_entoncesSeRechazaAunComoSuperusuario() {
        UUID id = insertar(evento(), "vicerrectorado.demo", null);

        assertThatThrownBy(
                        () ->
                                comoSuperusuario
                                        .sql(
                                                "update notificaciones.notificacion set titulo ="
                                                        + " 'Otro', leida_en = now() where id = :id")
                                        .param("id", id)
                                        .update())
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql(
                                                "update notificaciones.notificacion set titulo ="
                                                        + " 'Otro' where id = :id")
                                        .param("id", id)
                                        .update())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadaUnaNotificacion_cuandoSeIntentaBorrarOTruncar_entoncesSeRechaza() {
        UUID id = insertar(evento(), "vicerrectorado.demo", null);

        assertThatThrownBy(
                        () ->
                                comoSuperusuario
                                        .sql(
                                                "delete from notificaciones.notificacion where id = :id")
                                        .param("id", id)
                                        .update())
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                comoSuperusuario
                                        .sql("truncate notificaciones.notificacion cascade")
                                        .update())
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                comoProceso
                                        .sql("delete from notificaciones.evento_recibido")
                                        .update())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadoUnEvento_cuandoSeRepiteElDestinatario_entoncesSeRechazaSalvoConOtraCarrera() {
        UUID evento = evento();
        insertar(evento, "coordinacion.demo", "TI");

        assertThatThrownBy(() -> insertar(evento, "coordinacion.demo", "TI"))
                .isInstanceOf(DataAccessException.class);
        insertar(evento, "coordinacion.demo", "ADM");
        insertar(evento, "vicerrectorado.demo", null);
        assertThatThrownBy(() -> insertar(evento, "vicerrectorado.demo", null))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void dadoUnEnlaceOUnTituloFueraDeSuForma_cuandoSeInserta_entoncesSeRechaza() {
        UUID evento = evento();

        assertThatThrownBy(() -> insertar(evento, "a", null, "   ", "/consolidado"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertar(evento, "b", null, "Título", "https://otro.sitio"))
                .isInstanceOf(DataAccessException.class);
    }

    private static UUID evento() {
        UUID id = UUID.randomUUID();
        comoProceso
                .sql(
                        "insert into notificaciones.evento_recibido (id_evento, tipo, origen)"
                                + " values (:id, 'validacionocs.NotificacionesSolicitadas',"
                                + " 'validacionocs')")
                .param("id", id)
                .update();
        return id;
    }

    private static UUID insertar(UUID evento, String destinatario, String carrera) {
        return insertar(evento, destinatario, carrera, "El distributivo llegó", "/consolidado");
    }

    private static UUID insertar(
            UUID evento, String destinatario, String carrera, String titulo, String enlace) {
        UUID id = UUID.randomUUID();
        comoProceso
                .sql(
                        "insert into notificaciones.notificacion (id, id_evento, destinatario,"
                                + " carrera, periodo, titulo, detalle, enlace, autor,"
                                + " autor_nombre, momento) values (:id, :evento, :destinatario,"
                                + " :carrera, '2026-I', :titulo, 'Periodo 2026-I, versión 1.',"
                                + " :enlace, 'consolidacion.demo', 'Responsable', :momento)")
                .param("id", id)
                .param("evento", evento)
                .param("destinatario", destinatario)
                .param("carrera", carrera)
                .param("titulo", titulo)
                .param("enlace", enlace)
                .param("momento", Timestamp.from(Instant.now()))
                .update();
        return id;
    }
}
