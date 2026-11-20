package ec.mdd;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.notificaciones.puertos.entrada.EventoRecibido;
import ec.mdd.notificaciones.puertos.entrada.RecibirNotificaciones;
import ec.mdd.roles.puertos.entrada.AsignarCarrera;
import ec.mdd.roles.puertos.entrada.AsignarRolesIniciales;
import ec.mdd.roles.puertos.entrada.DesignarResponsable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Las notificaciones de Aprobación en el proceso identidad completo (F16-H01, T013): con Roles y
 * Usuarios reales, cada mensaje llega a los usuarios activos del rol, al designado del periodo o a
 * los coordinadores de la carrera devuelta, sin el autor.
 *
 * <p>La base es un PostgreSQL de Testcontainers que se descarta al terminar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ErrorTecnicoIntegrationTest.Contenedores.class)
class NotificacionesDesdeAprobacionIntegrationTest {

    private static final String TIPO = "validacionocs.NotificacionesSolicitadas";

    @Autowired private RecibirNotificaciones recibir;
    @Autowired private AsignarRolesIniciales rolesIniciales;
    @Autowired private AsignarCarrera carreras;
    @Autowired private DesignarResponsable designar;
    @Autowired private JdbcTemplate jdbc;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 6);

    @Test
    void dadoElEnvioARevision_cuandoLlega_entoncesLoRecibenLosVicerrectoresActivosSinElAutor() {
        String activo = usuario("vra.activo", true, "VICERRECTOR");
        String inactivo = usuario("vra.inactivo", false, "VICERRECTOR");
        String autor = usuario("vra.autor", true, "VICERRECTOR");
        String otroRol = usuario("rector", true, "RECTOR");

        recibir.recibir(evento(autor, mensaje("VICERRECTOR", null)));

        assertThat(bandeja(activo)).hasSize(1);
        assertThat(bandeja(activo).getFirst()).containsEntry("autor_nombre", "Nombre de " + autor);
        assertThat(bandeja(inactivo)).isEmpty();
        assertThat(bandeja(autor)).isEmpty();
        assertThat(bandeja(otroRol)).isEmpty();
    }

    @Test
    void dadoElResponsable_cuandoLlega_entoncesLoRecibenElRolFijoYElDesignadoDelPeriodo() {
        String periodo = "it-" + sufijo;
        jdbc.update(
                "INSERT INTO roles.periodo_conocido (codigo, estado, actualizado_en) VALUES (?,"
                        + " 'PLANIFICACION', now())"
                        + " ON CONFLICT DO NOTHING",
                periodo);
        String designado = usuario("designado", true, "DOCENTE");
        String otroDocente = usuario("otro.docente", true, "DOCENTE");
        String fijo = usuario("responsable.fijo", true, "RESPONSABLE_CONSOLIDACION");
        designar.designar(periodo, designado, "vicerrectorado.demo");

        recibir.recibir(evento("sistema", periodo, mensaje("RESPONSABLE_CONSOLIDACION", null)));

        assertThat(bandeja(designado))
                .singleElement()
                .satisfies(fila -> assertThat(fila).containsEntry("autor_nombre", "el sistema"));
        assertThat(bandeja(fijo)).hasSize(1);
        assertThat(bandeja(otroDocente)).isEmpty();
    }

    @Test
    void dadaUnaDevolucionDeDosCarreras_cuandoLlega_entoncesCadaCoordinadorRecibeLaSuya() {
        String ds = usuario("coord.ds", true, "COORDINADOR_CARRERA");
        String tds = usuario("coord.tds", true, "COORDINADOR_CARRERA");
        String otra = usuario("coord.otra", true, "COORDINADOR_CARRERA");
        String carreraDs = "DS" + sufijo.toUpperCase().substring(0, 3);
        String carreraTds = "TD" + sufijo.toUpperCase().substring(0, 3);
        carreras.indicar(ds, carreraDs, "admin.demo");
        carreras.indicar(tds, carreraTds, "admin.demo");
        carreras.indicar(otra, "OT" + sufijo.toUpperCase().substring(0, 3), "admin.demo");

        recibir.recibir(
                evento(
                        "consolidacion.demo",
                        mensaje("COORDINADOR_CARRERA", carreraDs),
                        mensaje("COORDINADOR_CARRERA", carreraTds)));

        assertThat(bandeja(ds))
                .singleElement()
                .satisfies(fila -> assertThat(fila).containsEntry("carrera", carreraDs));
        assertThat(bandeja(tds))
                .singleElement()
                .satisfies(fila -> assertThat(fila).containsEntry("carrera", carreraTds));
        assertThat(bandeja(otra)).isEmpty();
    }

    @Test
    void dadoElMismoEvento_cuandoLlegaDosVeces_entoncesLaBandejaTieneUna() {
        String activo = usuario("vra.dos", true, "VICERRECTOR");
        EventoRecibido evento = evento("consolidacion.demo", mensaje("VICERRECTOR", null));

        recibir.recibir(evento);
        recibir.recibir(evento);

        assertThat(bandeja(activo)).hasSize(1);
    }

    // F18-H01 (D-111): la publicación va a tres roles sin carrera; quien tiene dos recibe una.
    @Test
    void dadaLaPublicacion_cuandoLlega_entoncesUnaPorUsuarioAunqueTengaDosDeLosRoles() {
        String doble = usuario("docente.coordinador", true, "DOCENTE", "COORDINADOR_CARRERA");
        String talento = usuario("talento", true, "OPERADOR_DATOS");
        String inactivo = usuario("docente.inactivo", false, "DOCENTE");
        String autor = usuario("secretaria.autora", true, "SECRETARIA_GENERAL");
        String otraSecretaria = usuario("secretaria.otra", true, "SECRETARIA_GENERAL");

        recibir.recibir(
                evento(
                        autor,
                        mensaje("DOCENTE", null),
                        mensaje("OPERADOR_DATOS", null),
                        mensaje("COORDINADOR_CARRERA", null)));

        assertThat(bandeja(doble)).hasSize(1);
        assertThat(bandeja(talento)).hasSize(1);
        assertThat(bandeja(inactivo)).isEmpty();
        assertThat(bandeja(autor)).isEmpty();
        assertThat(bandeja(otraSecretaria)).isEmpty();
    }

    private String usuario(String base, boolean activo, String... roles) {
        String nombre = base + "." + sufijo;
        jdbc.update(
                "INSERT INTO usuarios.usuario (id, nombre_usuario, clave_hash, activo,"
                        + " nombre_para_mostrar) VALUES (?, ?, 'sin-clave', ?, ?)",
                UUID.randomUUID(),
                nombre,
                activo,
                "Nombre de " + nombre);
        rolesIniciales.asignar(nombre, List.of(roles));
        return nombre;
    }

    private List<Map<String, Object>> bandeja(String usuario) {
        return jdbc.queryForList(
                "SELECT destinatario, carrera, titulo, autor_nombre FROM"
                        + " notificaciones.notificacion WHERE destinatario = ?",
                usuario);
    }

    @SafeVarargs
    private EventoRecibido evento(String autor, Map<String, Object>... mensajes) {
        return evento(autor, "2026-I", mensajes);
    }

    @SafeVarargs
    private EventoRecibido evento(String autor, String periodo, Map<String, Object>... mensajes) {
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("periodo", periodo);
        contenido.put("autor", autor);
        contenido.put("momento", "2026-11-15T15:00:00Z");
        contenido.put("mensajes", new ArrayList<>(List.of(mensajes)));
        return new EventoRecibido(
                UUID.randomUUID(), TIPO, "validacionocs", Instant.now(), contenido);
    }

    private static Map<String, Object> mensaje(String rol, String carrera) {
        Map<String, Object> destino = new LinkedHashMap<>();
        destino.put("rol", rol);
        if (carrera != null) {
            destino.put("carrera", carrera);
        }
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("destino", destino);
        mensaje.put("titulo", "El distributivo llegó");
        mensaje.put("detalle", "Periodo 2026-I, versión 1.");
        mensaje.put("enlace", "/consolidado");
        return mensaje;
    }
}
