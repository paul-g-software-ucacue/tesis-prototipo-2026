package ec.mdd.docentes.adaptadores.salida;

import ec.mdd.docentes.puertos.salida.CambioRegistrado;
import ec.mdd.docentes.puertos.salida.RegistroDeCambios;
import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los cambios de los docentes en {@code docentes.registro_cambio}, con el detalle en {@code jsonb},
 * y cada uno también como el evento {@value #TIPO} hacia Usuarios, para la bitácora (F17-H01,
 * D-90).
 *
 * <p>La fila local no cambia. La entrada de la bitácora no lleva el valor de los datos personales
 * del docente (FR-014, D-91): esos campos se nombran en {@code objeto.camposPersonales} y se quitan
 * de {@code anterior}, de {@code nuevo} y de los {@code cambios} del objeto. Si un cambio solo tocó
 * datos personales, {@code anterior} y {@code nuevo} van vacíos: el cambio existió.
 */
public final class RegistroDeCambiosJdbc implements RegistroDeCambios {

    /**
     * El tipo del evento, con el contenido {@code EntradaBitacora} de {@code contratos/usuarios}.
     */
    public static final String TIPO = "docentes.EntradaBitacora";

    /** Los datos personales de un docente que la bitácora no guarda con su valor (D-91). */
    public static final Set<String> DATOS_PERSONALES =
            Set.of(
                    "primerNombre",
                    "segundoNombre",
                    "primerApellido",
                    "segundoApellido",
                    "correoInstitucional",
                    "telefono",
                    "condicionEspecial");

    private static final String ORIGEN = "docentes";
    private static final String DESTINO = "usuarios";

    private static final String REGISTRAR =
            "INSERT INTO docentes.registro_cambio (id, momento, usuario, accion, detalle)"
                    + " VALUES (:id, :momento, :usuario, :accion, CAST(:detalle AS jsonb))";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    /**
     * Registro sobre el {@link JdbcClient} del proceso.
     *
     * @param jdbc la base del proceso
     * @param eventos el publicador de Spring, que guarda el evento en la transacción en curso
     * @param reloj el momento de la publicación
     */
    public RegistroDeCambiosJdbc(JdbcClient jdbc, ApplicationEventPublisher eventos, Clock reloj) {
        this.jdbc = jdbc;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Override
    public void registrar(CambioRegistrado cambio) {
        jdbc.sql(REGISTRAR)
                .param("id", UUID.randomUUID())
                .param("momento", Timestamp.from(cambio.momento()))
                .param("usuario", cambio.usuario())
                .param("accion", cambio.accion().name())
                .param("detalle", JSON.writeValueAsString(cambio.detalle()))
                .update();
        eventos.publishEvent(
                EventoEntreProcesos.nuevo(TIPO, ORIGEN, DESTINO, entrada(cambio), reloj));
    }

    // Docentes, cargos y proyectos no pertenecen a un periodo (research §2): no lleva periodo.
    private static Map<String, Object> entrada(CambioRegistrado cambio) {
        Set<String> personales = new TreeSet<>();
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("usuario", cambio.usuario());
        entrada.put("accion", cambio.accion().name());
        entrada.put("momento", cambio.momento().toString());
        Map<String, Object> anterior = sinPersonales(cambio.anterior(), personales);
        Map<String, Object> nuevo = sinPersonales(cambio.nuevo(), personales);
        entrada.put("objeto", objeto(cambio.detalle(), personales));
        if (!cambio.anterior().isEmpty()) {
            entrada.put("anterior", anterior);
        }
        if (!cambio.nuevo().isEmpty()) {
            entrada.put("nuevo", nuevo);
        }
        return entrada;
    }

    private static Map<String, Object> sinPersonales(
            Map<String, Object> valores, Set<String> personales) {
        Map<String, Object> sin = new LinkedHashMap<>();
        valores.forEach(
                (campo, valor) -> {
                    if (DATOS_PERSONALES.contains(campo)) {
                        personales.add(campo);
                    } else {
                        sin.put(campo, valor);
                    }
                });
        return sin;
    }

    // El detalle local, sin los datos personales de sus cambios y con los campos que se quitaron.
    private static Map<String, Object> objeto(Map<String, Object> detalle, Set<String> personales) {
        Map<String, Object> objeto = sinPersonales(detalle, personales);
        if (detalle.get("cambios") instanceof List<?> cambios) {
            List<Object> sin = new ArrayList<>();
            for (Object uno : cambios) {
                if (uno instanceof Map<?, ?> cambio
                        && cambio.get("campo") instanceof String campo
                        && DATOS_PERSONALES.contains(campo)) {
                    personales.add(campo);
                } else {
                    sin.add(uno);
                }
            }
            objeto.put("cambios", sin);
        }
        if (!personales.isEmpty()) {
            objeto.put("camposPersonales", List.copyOf(personales));
        }
        return objeto;
    }
}
