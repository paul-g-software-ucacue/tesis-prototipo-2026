package ec.mdd.academico.adaptadores.salida;

import ec.mdd.academico.dominio.Matricula;
import ec.mdd.academico.puertos.salida.RepositorioMatriculas;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Los matriculados en {@code academico.matricula} (V6), con {@link JdbcClient}. */
public final class RepositorioMatriculasJdbc implements RepositorioMatriculas {

    private final JdbcClient jdbc;

    /** Recibe el cliente de la base del proceso. */
    public RepositorioMatriculasJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Matricula> deLosPeriodos(List<String> periodos) {
        if (periodos.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(
                        "SELECT periodo, carrera, total, fecha_corte, fuente FROM academico.matricula"
                                + " WHERE periodo IN (:periodos) ORDER BY periodo, carrera")
                .param("periodos", periodos)
                .query((fila, numero) -> matricula(fila))
                .list();
    }

    @Override
    public void bloquear() {
        jdbc.sql("LOCK TABLE academico.matricula IN SHARE ROW EXCLUSIVE MODE").update();
    }

    @Override
    public void registrar(Matricula matricula, String quien, Instant momento) {
        jdbc.sql(
                        "INSERT INTO academico.matricula (periodo, carrera, total, fecha_corte, fuente,"
                                + " registrada_en, registrada_por) VALUES (:periodo, :carrera, :total,"
                                + " :corte, :fuente, :momento, :quien)")
                .param("periodo", matricula.periodo())
                .param("carrera", matricula.carrera())
                .param("total", matricula.total())
                .param("corte", Date.valueOf(matricula.fechaCorte()))
                .param("fuente", matricula.fuente())
                .param("momento", Timestamp.from(momento))
                .param("quien", quien)
                .update();
    }

    @Override
    public void guardar(Matricula matricula) {
        jdbc.sql(
                        "UPDATE academico.matricula SET total = :total, fecha_corte = :corte, fuente ="
                                + " :fuente WHERE periodo = :periodo AND carrera = :carrera")
                .param("periodo", matricula.periodo())
                .param("carrera", matricula.carrera())
                .param("total", matricula.total())
                .param("corte", Date.valueOf(matricula.fechaCorte()))
                .param("fuente", matricula.fuente())
                .update();
    }

    private static Matricula matricula(ResultSet fila) throws SQLException {
        return new Matricula(
                fila.getString("periodo"),
                fila.getString("carrera"),
                fila.getInt("total"),
                fila.getDate("fecha_corte").toLocalDate(),
                fila.getString("fuente"));
    }
}
