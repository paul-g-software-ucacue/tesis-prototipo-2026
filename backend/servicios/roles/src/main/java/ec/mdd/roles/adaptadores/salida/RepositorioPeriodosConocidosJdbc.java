package ec.mdd.roles.adaptadores.salida;

import ec.mdd.roles.dominio.EstadoConocido;
import ec.mdd.roles.dominio.PeriodoConocido;
import ec.mdd.roles.puertos.salida.RepositorioPeriodosConocidos;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/** La copia de los periodos en {@code roles.periodo_conocido}. */
public final class RepositorioPeriodosConocidosJdbc implements RepositorioPeriodosConocidos {

    private static final String BUSCAR =
            "SELECT codigo, estado FROM roles.periodo_conocido WHERE codigo = :codigo";

    private static final String GUARDAR =
            "INSERT INTO roles.periodo_conocido (codigo, estado, actualizado_en) VALUES (:codigo,"
                    + " :estado, :momento) ON CONFLICT (codigo) DO UPDATE SET estado ="
                    + " excluded.estado, actualizado_en = excluded.actualizado_en";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioPeriodosConocidosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<PeriodoConocido> buscar(String codigo) {
        return consultar(BUSCAR, codigo);
    }

    @Override
    public Optional<PeriodoConocido> bloquear(String codigo) {
        return consultar(BUSCAR + " FOR UPDATE", codigo);
    }

    @Override
    public void guardar(PeriodoConocido periodo, Instant momento) {
        jdbc.sql(GUARDAR)
                .param("codigo", periodo.codigo())
                .param("estado", periodo.estado().name())
                .param("momento", Timestamp.from(momento))
                .update();
    }

    private Optional<PeriodoConocido> consultar(String sql, String codigo) {
        return jdbc.sql(sql)
                .param("codigo", codigo)
                .query(
                        (fila, numero) ->
                                new PeriodoConocido(
                                        fila.getString("codigo"),
                                        EstadoConocido.valueOf(fila.getString("estado"))))
                .optional();
    }
}
