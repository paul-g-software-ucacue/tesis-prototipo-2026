package ec.mdd.academico.adaptadores.salida;

import ec.mdd.academico.dominio.AsignaturaParalelo;
import ec.mdd.academico.dominio.EstadoDeAsignaturaParalelo;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.puertos.salida.RepositorioOferta;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * La oferta de cada periodo en {@code academico.asignatura_paralelo}, con {@link JdbcClient}. Sin
 * sede ni horas, las columnas quedan nulas: son las de la carrera y las de la asignatura.
 */
public final class RepositorioOfertaJdbc implements RepositorioOferta {

    private static final String DEL_PERIODO =
            "SELECT periodo, asignatura, paralelo, jornada, modalidad, sede, estudiantes,"
                    + " horas_semanales, estado FROM academico.asignatura_paralelo"
                    + " WHERE periodo = :periodo ORDER BY asignatura, paralelo";

    // Choca consigo mismo y con las escrituras, pero no con las lecturas.
    private static final String BLOQUEAR =
            "LOCK TABLE academico.asignatura_paralelo IN SHARE ROW EXCLUSIVE MODE";

    private static final String REGISTRAR =
            "INSERT INTO academico.asignatura_paralelo (periodo, asignatura, paralelo, jornada,"
                    + " modalidad, sede, estudiantes, horas_semanales, estado, registrada_en,"
                    + " registrada_por) VALUES (:periodo, :asignatura, :paralelo, :jornada,"
                    + " :modalidad, :sede, :estudiantes, :horas, :estado, :momento, :quien)";

    private static final String GUARDAR =
            "UPDATE academico.asignatura_paralelo SET jornada = :jornada, modalidad = :modalidad,"
                    + " sede = :sede, estudiantes = :estudiantes, horas_semanales = :horas,"
                    + " estado = :estado WHERE periodo = :periodo AND asignatura = :asignatura"
                    + " AND paralelo = :paralelo";

    private final JdbcClient jdbc;

    public RepositorioOfertaJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AsignaturaParalelo> delPeriodo(String periodo) {
        return jdbc.sql(DEL_PERIODO)
                .param("periodo", periodo)
                .query(RepositorioOfertaJdbc::asignaturaParalelo)
                .list();
    }

    @Override
    public void bloquear() {
        jdbc.sql(BLOQUEAR).update();
    }

    @Override
    public void registrar(AsignaturaParalelo asignaturaParalelo, String quien, Instant momento) {
        conDatos(jdbc.sql(REGISTRAR), asignaturaParalelo)
                .param("momento", Timestamp.from(momento))
                .param("quien", quien)
                .update();
    }

    @Override
    public void guardar(AsignaturaParalelo asignaturaParalelo) {
        conDatos(jdbc.sql(GUARDAR), asignaturaParalelo).update();
    }

    private static JdbcClient.StatementSpec conDatos(
            JdbcClient.StatementSpec sentencia, AsignaturaParalelo asignaturaParalelo) {
        return sentencia
                .param("periodo", asignaturaParalelo.periodo())
                .param("asignatura", asignaturaParalelo.asignatura())
                .param("paralelo", asignaturaParalelo.paralelo())
                .param("jornada", asignaturaParalelo.jornada().name())
                .param("modalidad", asignaturaParalelo.modalidad().name())
                .param("sede", asignaturaParalelo.sede())
                .param("estudiantes", asignaturaParalelo.estudiantes())
                .param("horas", asignaturaParalelo.horasIndicadas())
                .param("estado", asignaturaParalelo.estado().name());
    }

    private static AsignaturaParalelo asignaturaParalelo(ResultSet fila, int numero)
            throws SQLException {
        return new AsignaturaParalelo(
                fila.getString("periodo"),
                fila.getString("asignatura"),
                fila.getString("paralelo"),
                Jornada.valueOf(fila.getString("jornada")),
                Modalidad.valueOf(fila.getString("modalidad")),
                fila.getString("sede"),
                fila.getInt("estudiantes"),
                fila.getBigDecimal("horas_semanales"),
                EstadoDeAsignaturaParalelo.valueOf(fila.getString("estado")));
    }
}
