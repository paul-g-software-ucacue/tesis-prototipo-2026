package ec.mdd.roles.adaptadores.salida;

import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import ec.mdd.roles.puertos.salida.CambioDeRoles;
import ec.mdd.roles.puertos.salida.RegistroDeCambios;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los cambios de roles en {@code roles.registro_cambio}, con el detalle en {@code jsonb}, y cada
 * uno también como el evento {@value #TIPO} hacia Usuarios, para la bitácora (F17-H01, D-90).
 *
 * <p>La fila local no cambia de forma. El evento lo guarda el registro persistente de publicaciones
 * en la transacción en curso, y se entrega después de confirmarla (ADR-018). Roles y Usuarios viven
 * en {@code identidad}: la entrega es local, sin HTTP, y Roles no conoce a Usuarios (ADR-024).
 */
public final class RegistroDeCambiosJdbc implements RegistroDeCambios {

    /**
     * El tipo del evento, con el contenido {@code EntradaBitacora} de {@code contratos/usuarios}.
     */
    public static final String TIPO = "roles.EntradaBitacora";

    private static final String ORIGEN = "roles";
    private static final String DESTINO = "usuarios";

    private static final String REGISTRAR =
            "INSERT INTO roles.registro_cambio (id, momento, usuario, accion, detalle)"
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
    public void registrar(CambioDeRoles cambio) {
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

    private static Map<String, Object> entrada(CambioDeRoles cambio) {
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("usuario", cambio.usuario());
        entrada.put("accion", cambio.accion().name());
        entrada.put("momento", cambio.momento().toString());
        // Solo la designación es de un periodo (research §2).
        if (cambio.accion() == CambioDeRoles.Accion.DESIGNAR_RESPONSABLE
                && cambio.detalle().get("periodo") instanceof String periodo) {
            entrada.put("periodo", periodo);
        }
        entrada.put("objeto", cambio.detalle());
        if (!cambio.anterior().isEmpty()) {
            entrada.put("anterior", cambio.anterior());
        }
        if (!cambio.nuevo().isEmpty()) {
            entrada.put("nuevo", cambio.nuevo());
        }
        return entrada;
    }
}
