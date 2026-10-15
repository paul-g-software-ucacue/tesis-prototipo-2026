package ec.mdd.usuarios.adaptadores.salida;

import ec.mdd.usuarios.puertos.entrada.EventoRecibido;
import ec.mdd.usuarios.puertos.salida.RepositorioEventosRecibidos;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Los eventos recibidos en {@code usuarios.evento_recibido}, con {@code INSERT … ON CONFLICT DO
 * NOTHING}: la clave primaria decide cuál de dos entregas simultáneas registra el evento, sin leer
 * antes ni bloquear (research §5).
 */
public final class RepositorioEventosRecibidosJdbc implements RepositorioEventosRecibidos {

    private static final String INSERTAR =
            "INSERT INTO usuarios.evento_recibido (id_evento, tipo, origen, momento)"
                    + " VALUES (:id, :tipo, :origen, :momento)"
                    + " ON CONFLICT (id_evento) DO NOTHING";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioEventosRecibidosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean registrarSiEsNuevo(EventoRecibido evento) {
        return jdbc.sql(INSERTAR)
                        .param("id", evento.idEvento())
                        .param("tipo", evento.tipo())
                        .param("origen", evento.origen())
                        .param("momento", Timestamp.from(evento.momento()))
                        .update()
                == 1;
    }
}
