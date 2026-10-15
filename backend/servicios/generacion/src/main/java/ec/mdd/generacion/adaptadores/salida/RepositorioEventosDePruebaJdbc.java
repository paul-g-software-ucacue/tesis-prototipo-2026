package ec.mdd.generacion.adaptadores.salida;

import ec.mdd.generacion.puertos.salida.RepositorioEventosDePrueba;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Los eventos de prueba en {@code generacion.evento_prueba} (D-48). */
public final class RepositorioEventosDePruebaJdbc implements RepositorioEventosDePrueba {

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioEventosDePruebaJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(UUID idEvento, Instant momento) {
        jdbc.sql("insert into generacion.evento_prueba (id_evento, momento) values (?, ?)")
                .params(idEvento, Timestamp.from(momento))
                .update();
    }
}
