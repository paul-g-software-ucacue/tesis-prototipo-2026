package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.notificaciones.puertos.entrada.EventoRecibido;
import ec.mdd.notificaciones.puertos.entrada.RecibirNotificaciones;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * La notificación de firma pendiente en el proceso identidad completo (F16-H02, T010): con Usuarios
 * real, el destino por identificación llega a la cuenta activa que la tiene, y a nadie más; sin
 * cuenta activa, el evento queda recibido y no hay notificación (D-110).
 *
 * <p>La base es un PostgreSQL de Testcontainers que se descarta al terminar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ErrorTecnicoIntegrationTest.Contenedores.class)
class NotificacionesDesdeFirmaIntegrationTest {

    private static final String TIPO = "firma.NotificacionesSolicitadas";

    @Autowired private RecibirNotificaciones recibir;
    @Autowired private JdbcTemplate jdbc;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 6);

    @Test
    void dadaLaIdentificacionDeUnaCuentaActiva_cuandoLlega_entoncesLaRecibeConElAutor() {
        String autor = usuario("firmante.uno", true, null);
        String identificacion = identificacion();
        String siguiente = usuario("firmante.dos", true, identificacion);
        String otro = usuario("otro", true, identificacion());

        recibir.recibir(evento(autor, Map.of("identificacion", identificacion)));

        assertThat(bandeja(siguiente))
                .singleElement()
                .satisfies(
                        fila -> {
                            assertThat(fila)
                                    .containsEntry("autor_nombre", "Nombre de " + autor)
                                    .containsEntry(
                                            "titulo", "Tienes una firma pendiente: «Revisado por»")
                                    .containsEntry("enlace", "/firmas-pendientes");
                            assertThat(fila.get("carrera")).isNull();
                        });
        assertThat(bandeja(otro)).isEmpty();
        assertThat(bandeja(autor)).isEmpty();
    }

    @Test
    void dadaUnaIdentificacionSinCuenta_cuandoLlega_entoncesQuedaRecibidoSinNotificacion() {
        String autor = usuario("firmante.tres", true, null);
        EventoRecibido evento = evento(autor, Map.of("identificacion", identificacion()));

        recibir.recibir(evento);

        assertThat(recibido(evento)).isTrue();
        assertThat(notificacionesDe(evento)).isZero();
    }

    @Test
    void dadaLaIdentificacionDeUnaCuentaDesactivada_cuandoLlega_entoncesNoHayNotificacion() {
        String autor = usuario("firmante.cuatro", true, null);
        String identificacion = identificacion();
        String desactivada = usuario("desactivada", false, identificacion);
        EventoRecibido evento = evento(autor, Map.of("identificacion", identificacion));

        recibir.recibir(evento);

        assertThat(recibido(evento)).isTrue();
        assertThat(bandeja(desactivada)).isEmpty();
    }

    @Test
    void dadoUnDestinoPorUsuario_cuandoLlega_entoncesLaRecibeEsaCuenta() {
        String autor = usuario("firmante.cinco", true, null);
        String designado = usuario("designado", true, null);

        recibir.recibir(evento(autor, Map.of("usuario", designado)));

        assertThat(bandeja(designado)).hasSize(1);
    }

    private String usuario(String base, boolean activo, String identificacion) {
        String nombre = base + "." + sufijo;
        jdbc.update(
                "INSERT INTO usuarios.usuario (id, nombre_usuario, clave_hash, activo,"
                        + " nombre_para_mostrar, identificacion) VALUES (?, ?, 'sin-clave', ?, ?, ?)",
                UUID.randomUUID(),
                nombre,
                activo,
                "Nombre de " + nombre,
                identificacion);
        return nombre;
    }

    // Una identificación ficticia que no choca con las de otras pruebas: es única entre cuentas.
    private static String identificacion() {
        return "9" + ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
    }

    private List<Map<String, Object>> bandeja(String usuario) {
        return jdbc.queryForList(
                "SELECT destinatario, carrera, titulo, enlace, autor_nombre FROM"
                        + " notificaciones.notificacion WHERE destinatario = ?",
                usuario);
    }

    private boolean recibido(EventoRecibido evento) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT count(*) = 1 FROM notificaciones.evento_recibido WHERE id_evento ="
                                + " ?",
                        Boolean.class,
                        evento.idEvento()));
    }

    private int notificacionesDe(EventoRecibido evento) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM notificaciones.notificacion WHERE id_evento = ?",
                Integer.class,
                evento.idEvento());
    }

    private static EventoRecibido evento(String autor, Map<String, Object> destino) {
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("destino", new LinkedHashMap<>(destino));
        mensaje.put("titulo", "Tienes una firma pendiente: «Revisado por»");
        mensaje.put("detalle", "Periodo 2026-I, versión 1. «Elaborado por» la firmó Uno.");
        mensaje.put("enlace", "/firmas-pendientes");
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("periodo", "2026-I");
        contenido.put("autor", autor);
        contenido.put("momento", "2026-11-18T15:04:05Z");
        contenido.put("mensajes", new ArrayList<>(List.of(mensaje)));
        return new EventoRecibido(UUID.randomUUID(), TIPO, "firma", Instant.now(), contenido);
    }
}
