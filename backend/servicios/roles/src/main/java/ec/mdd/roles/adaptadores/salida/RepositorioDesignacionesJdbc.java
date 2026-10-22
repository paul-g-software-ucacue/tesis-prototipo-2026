package ec.mdd.roles.adaptadores.salida;

import ec.mdd.roles.dominio.Designacion;
import ec.mdd.roles.puertos.salida.RepositorioDesignaciones;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Las designaciones en {@code roles.designacion_responsable}, una por periodo. */
public final class RepositorioDesignacionesJdbc implements RepositorioDesignaciones {

    private static final String COLUMNAS =
            "SELECT periodo, usuario, designado_por, designado_en FROM"
                    + " roles.designacion_responsable";

    private static final String GUARDAR =
            "INSERT INTO roles.designacion_responsable (periodo, usuario, designado_en,"
                    + " designado_por) VALUES (:periodo, :usuario, :momento, :quien) ON CONFLICT"
                    + " (periodo) DO UPDATE SET usuario = excluded.usuario, designado_en ="
                    + " excluded.designado_en, designado_por = excluded.designado_por";

    private static final String ABIERTOS =
            "SELECT d.periodo FROM roles.designacion_responsable d JOIN roles.periodo_conocido p"
                    + " ON p.codigo = d.periodo WHERE d.usuario = :usuario AND p.estado <>"
                    + " 'CERRADO' ORDER BY d.periodo";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioDesignacionesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Designacion> todas() {
        return jdbc.sql(COLUMNAS + " ORDER BY periodo")
                .query(RepositorioDesignacionesJdbc::designacion)
                .list();
    }

    @Override
    public Optional<Designacion> dePeriodo(String periodo) {
        return jdbc.sql(COLUMNAS + " WHERE periodo = :periodo")
                .param("periodo", periodo)
                .query(RepositorioDesignacionesJdbc::designacion)
                .optional();
    }

    @Override
    public void guardar(Designacion designacion) {
        jdbc.sql(GUARDAR)
                .param("periodo", designacion.periodo())
                .param("usuario", designacion.usuario())
                .param("momento", Timestamp.from(designacion.designadoEn()))
                .param("quien", designacion.designadoPor())
                .update();
    }

    @Override
    public List<String> periodosAbiertosDe(String usuario) {
        return jdbc.sql(ABIERTOS).param("usuario", usuario).query(String.class).list();
    }

    private static Designacion designacion(ResultSet fila, int numero) throws SQLException {
        return new Designacion(
                fila.getString("periodo"),
                fila.getString("usuario"),
                fila.getString("designado_por"),
                fila.getTimestamp("designado_en").toInstant());
    }
}
