package ec.mdd.notificaciones.adaptadores.salida;

import ec.mdd.notificaciones.dominio.Notificacion;
import ec.mdd.notificaciones.puertos.salida.RepositorioNotificaciones;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Las bandejas en {@code notificaciones.notificacion} (F16-H01, data-model.md). Solo inserta, lee y
 * marca como leída: la base rechaza cualquier otro cambio. Una notificación repetida para el mismo
 * evento, destinatario y carrera no se inserta otra vez ({@code ON CONFLICT DO NOTHING}), también
 * con dos entregas a la vez (FR-012).
 */
public final class RepositorioNotificacionesJdbc implements RepositorioNotificaciones {

    private static final String COLUMNAS =
            "id, id_evento, destinatario, carrera, periodo, titulo, detalle, enlace, autor,"
                    + " autor_nombre, momento, recibida_en, leida_en";

    private static final String INSERTAR =
            "INSERT INTO notificaciones.notificacion (id, id_evento, destinatario, carrera,"
                    + " periodo, titulo, detalle, enlace, autor, autor_nombre, momento,"
                    + " recibida_en) VALUES (:id, :evento, :destinatario, :carrera, :periodo,"
                    + " :titulo, :detalle, :enlace, :autor, :autorNombre, :momento, :recibidaEn)"
                    + " ON CONFLICT (id_evento, destinatario, coalesce(carrera, '')) DO NOTHING";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioNotificacionesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(List<Notificacion> notificaciones) {
        for (Notificacion notificacion : notificaciones) {
            jdbc.sql(INSERTAR)
                    .param("id", notificacion.id())
                    .param("evento", notificacion.idEvento())
                    .param("destinatario", notificacion.destinatario())
                    .param("carrera", notificacion.carrera().orElse(null), Types.VARCHAR)
                    .param("periodo", notificacion.periodo())
                    .param("titulo", notificacion.titulo())
                    .param("detalle", notificacion.detalle())
                    .param("enlace", notificacion.enlace())
                    .param("autor", notificacion.autor())
                    .param("autorNombre", notificacion.autorNombre())
                    .param("momento", Timestamp.from(notificacion.momento()))
                    .param("recibidaEn", Timestamp.from(notificacion.recibidaEn()))
                    .update();
        }
    }

    @Override
    public List<Notificacion> bandeja(String destinatario, int limite) {
        return jdbc.sql(
                        "SELECT "
                                + COLUMNAS
                                + " FROM notificaciones.notificacion WHERE destinatario ="
                                + " :destinatario ORDER BY momento DESC, id LIMIT :limite")
                .param("destinatario", destinatario)
                .param("limite", limite)
                .query(RepositorioNotificacionesJdbc::fila)
                .list();
    }

    @Override
    public int sinLeer(String destinatario) {
        return jdbc.sql(
                        "SELECT count(*) FROM notificaciones.notificacion WHERE destinatario ="
                                + " :destinatario AND leida_en IS NULL")
                .param("destinatario", destinatario)
                .query(Integer.class)
                .single();
    }

    @Override
    public Optional<Notificacion> de(UUID id, String destinatario) {
        return jdbc.sql(
                        "SELECT "
                                + COLUMNAS
                                + " FROM notificaciones.notificacion WHERE id = :id AND"
                                + " destinatario = :destinatario")
                .param("id", id)
                .param("destinatario", destinatario)
                .query(RepositorioNotificacionesJdbc::fila)
                .optional();
    }

    @Override
    public void marcarLeida(UUID id, Instant momento) {
        jdbc.sql(
                        "UPDATE notificaciones.notificacion SET leida_en = :momento WHERE id = :id"
                                + " AND leida_en IS NULL")
                .param("id", id)
                .param("momento", Timestamp.from(momento))
                .update();
    }

    private static Notificacion fila(ResultSet fila, int numero) throws SQLException {
        Timestamp leida = fila.getTimestamp("leida_en");
        return new Notificacion(
                fila.getObject("id", UUID.class),
                fila.getObject("id_evento", UUID.class),
                fila.getString("destinatario"),
                Optional.ofNullable(fila.getString("carrera")),
                fila.getString("periodo"),
                fila.getString("titulo"),
                fila.getString("detalle"),
                fila.getString("enlace"),
                fila.getString("autor"),
                fila.getString("autor_nombre"),
                fila.getTimestamp("momento").toInstant(),
                fila.getTimestamp("recibida_en").toInstant(),
                Optional.ofNullable(leida).map(Timestamp::toInstant));
    }
}
