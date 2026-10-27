package ec.mdd.academico.adaptadores.salida;

import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Los periodos académicos en {@code academico.periodo_academico}, con {@link JdbcClient}. Un código
 * repetido no se guarda: lo decide la clave primaria, sin una consulta previa que otra transacción
 * podría adelantar.
 */
public final class RepositorioPeriodosJdbc implements RepositorioPeriodos {

    private static final String COLUMNAS =
            "SELECT codigo, nombre, inicio, fin, estado FROM academico.periodo_academico";

    private static final String REGISTRAR =
            "INSERT INTO academico.periodo_academico (codigo, nombre, inicio, fin, estado,"
                    + " registrado_en, registrado_por) VALUES (:codigo, :nombre, :inicio, :fin,"
                    + " :estado, :momento, :quien) ON CONFLICT (codigo) DO NOTHING";

    private static final String GUARDAR =
            "UPDATE academico.periodo_academico SET nombre = :nombre, inicio = :inicio, fin ="
                    + " :fin, estado = :estado WHERE codigo = :codigo";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioPeriodosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<PeriodoAcademico> todos() {
        return jdbc.sql(COLUMNAS + " ORDER BY inicio DESC, codigo")
                .query(RepositorioPeriodosJdbc::periodo)
                .list();
    }

    @Override
    public Optional<PeriodoAcademico> porCodigo(String codigo) {
        return jdbc.sql(COLUMNAS + " WHERE codigo = :codigo")
                .param("codigo", codigo)
                .query(RepositorioPeriodosJdbc::periodo)
                .optional();
    }

    @Override
    public void bloquearTodos() {
        jdbc.sql("LOCK TABLE academico.periodo_academico IN SHARE ROW EXCLUSIVE MODE").update();
    }

    @Override
    public Optional<PeriodoAcademico> bloquear(String codigo) {
        return jdbc.sql(COLUMNAS + " WHERE codigo = :codigo FOR UPDATE")
                .param("codigo", codigo)
                .query(RepositorioPeriodosJdbc::periodo)
                .optional();
    }

    @Override
    public boolean registrar(PeriodoAcademico periodo, String quien, Instant momento) {
        return jdbc.sql(REGISTRAR)
                        .param("codigo", periodo.codigo())
                        .param("nombre", periodo.nombre())
                        .param("inicio", periodo.inicio())
                        .param("fin", periodo.fin())
                        .param("estado", periodo.estado().name())
                        .param("momento", Timestamp.from(momento))
                        .param("quien", quien)
                        .update()
                == 1;
    }

    @Override
    public void guardar(PeriodoAcademico periodo) {
        jdbc.sql(GUARDAR)
                .param("codigo", periodo.codigo())
                .param("nombre", periodo.nombre())
                .param("inicio", periodo.inicio())
                .param("fin", periodo.fin())
                .param("estado", periodo.estado().name())
                .update();
    }

    private static PeriodoAcademico periodo(ResultSet fila, int numero) throws SQLException {
        return new PeriodoAcademico(
                fila.getString("codigo"),
                fila.getString("nombre"),
                fila.getObject("inicio", LocalDate.class),
                fila.getObject("fin", LocalDate.class),
                EstadoPeriodo.valueOf(fila.getString("estado")));
    }
}
