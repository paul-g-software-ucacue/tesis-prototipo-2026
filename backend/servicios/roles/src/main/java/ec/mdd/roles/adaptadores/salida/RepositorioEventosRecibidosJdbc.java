package ec.mdd.roles.adaptadores.salida;

import ec.mdd.roles.puertos.salida.RepositorioEventosRecibidos;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Los eventos recibidos en {@code roles.evento_recibido}: {@code ON CONFLICT DO NOTHING} decide si
 * es nuevo, sin una consulta previa que otra entrega podría adelantar (ADR-018).
 */
public final class RepositorioEventosRecibidosJdbc implements RepositorioEventosRecibidos {

    private static final String REGISTRAR =
            "INSERT INTO roles.evento_recibido (id_evento, tipo, origen, recibido_en) VALUES"
                    + " (:id, :tipo, :origen, :momento) ON CONFLICT (id_evento) DO NOTHING";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioEventosRecibidosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean registrarSiEsNuevo(UUID idEvento, String tipo, String origen, Instant momento) {
        return jdbc.sql(REGISTRAR)
                        .param("id", idEvento)
                        .param("tipo", tipo)
                        .param("origen", origen)
                        .param("momento", Timestamp.from(momento))
                        .update()
                == 1;
    }
}
