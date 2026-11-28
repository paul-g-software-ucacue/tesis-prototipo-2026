package ec.mdd.reporting.adaptadores.salida;

import ec.mdd.reporting.dominio.ExportacionSiga;
import ec.mdd.reporting.dominio.SumaDeVerificacion;
import ec.mdd.reporting.puertos.salida.ExportacionesSiga;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Las exportaciones a SIGA en {@code reporting.exportacion_siga} (V3, F19-H02, D-120). Cada una
 * tiene su propio identificador, así que dos exportaciones a la vez no chocan.
 */
public final class ExportacionesSigaJdbc implements ExportacionesSiga {

    private static final String GUARDAR =
            "INSERT INTO reporting.exportacion_siga (id, periodo, version, hash_version, layout,"
                    + " nombre, filas, sha256, exportada_por, exportada_en) VALUES (:id, :periodo,"
                    + " :version, :hash, :layout, :nombre, :filas, :sha256, :quien, :momento)";

    private static final String DEL_PERIODO =
            "SELECT id, periodo, version, hash_version, layout, nombre, filas, sha256,"
                    + " exportada_por, exportada_en FROM reporting.exportacion_siga"
                    + " WHERE periodo = :periodo ORDER BY exportada_en DESC, id";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public ExportacionesSigaJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(ExportacionSiga exportacion) {
        jdbc.sql(GUARDAR)
                .param("id", exportacion.id())
                .param("periodo", exportacion.periodo())
                .param("version", exportacion.version())
                .param("hash", exportacion.hashVersion())
                .param("layout", exportacion.layout())
                .param("nombre", exportacion.nombre())
                .param("filas", exportacion.filas())
                .param("sha256", exportacion.suma().valor())
                .param("quien", exportacion.usuario())
                .param("momento", Timestamp.from(exportacion.momento()))
                .update();
    }

    @Override
    public List<ExportacionSiga> delPeriodo(String periodo) {
        return jdbc.sql(DEL_PERIODO)
                .param("periodo", periodo)
                .query(ExportacionesSigaJdbc::fila)
                .list();
    }

    private static ExportacionSiga fila(ResultSet fila, int numero) throws SQLException {
        return new ExportacionSiga(
                fila.getObject("id", UUID.class),
                fila.getString("exportada_por"),
                fila.getTimestamp("exportada_en").toInstant(),
                fila.getString("periodo"),
                fila.getInt("version"),
                fila.getString("hash_version"),
                fila.getInt("layout"),
                fila.getString("nombre"),
                fila.getInt("filas"),
                new SumaDeVerificacion(fila.getString("sha256")));
    }
}
