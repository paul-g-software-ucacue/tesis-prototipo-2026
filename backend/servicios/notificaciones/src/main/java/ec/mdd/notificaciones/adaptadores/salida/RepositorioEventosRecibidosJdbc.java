package ec.mdd.notificaciones.adaptadores.salida;

import ec.mdd.notificaciones.puertos.entrada.EventoRecibido;
import ec.mdd.notificaciones.puertos.salida.RepositorioEventosRecibidos;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Los eventos recibidos en {@code notificaciones.evento_recibido}, con {@code INSERT … ON CONFLICT
 * DO NOTHING}: la clave primaria decide cuál de dos entregas simultáneas registra el evento, sin
 * leer antes ni bloquear (ADR-018 §4).
 */
public final class RepositorioEventosRecibidosJdbc implements RepositorioEventosRecibidos {

    private static final String INSERTAR =
            "INSERT INTO notificaciones.evento_recibido (id_evento, tipo, origen)"
                    + " VALUES (:id, :tipo, :origen) ON CONFLICT (id_evento) DO NOTHING";

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
                        .update()
                == 1;
    }
}
