package ec.mdd.reporting.adaptadores.salida;

import ec.mdd.reporting.puertos.salida.LayoutsGuardados;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Las versiones del layout de exportación en {@code reporting.layout_exportacion} (V2), y la
 * plantilla del recurso {@code reporting/layout-siga-por-defecto.yaml} (F19-H01, D-119).
 */
public final class LayoutsGuardadosJdbc implements LayoutsGuardados {

    static final String RECURSO = "/reporting/layout-siga-por-defecto.yaml";

    private static final String COLUMNAS =
            "SELECT numero, texto, sha256, cargado_por, cargado_en FROM reporting.layout_exportacion";

    private static final String GUARDAR =
            "INSERT INTO reporting.layout_exportacion"
                    + " (numero, texto, sha256, cargado_por, cargado_en)"
                    + " SELECT coalesce(max(numero), 0) + 1, :texto, :sha256, :quien, :momento"
                    + " FROM reporting.layout_exportacion RETURNING numero";

    private final JdbcClient jdbc;
    private final String porDefecto;

    /** Repositorio sobre el {@link JdbcClient} del proceso; lee la plantilla una vez. */
    public LayoutsGuardadosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
        this.porDefecto = plantilla();
    }

    @Override
    public Optional<LayoutGuardado> vigente() {
        return jdbc.sql(COLUMNAS + " ORDER BY numero DESC LIMIT 1")
                .query(LayoutsGuardadosJdbc::fila)
                .optional();
    }

    @Override
    public List<LayoutGuardado> todos() {
        return jdbc.sql(COLUMNAS + " ORDER BY numero DESC")
                .query(LayoutsGuardadosJdbc::fila)
                .list();
    }

    // Dos cargas a la vez calculan el mismo número; la clave primaria rechaza la segunda, que
    // reintenta una vez con el número nuevo (research §8). Con una sola Secretaría no debería
    // pasar.
    @Override
    public int guardar(String texto, String sha256, String quien, Instant momento) {
        try {
            return insertar(texto, sha256, quien, momento);
        } catch (DuplicateKeyException choque) {
            return insertar(texto, sha256, quien, momento);
        }
    }

    @Override
    public String porDefecto() {
        return porDefecto;
    }

    private int insertar(String texto, String sha256, String quien, Instant momento) {
        return jdbc.sql(GUARDAR)
                .param("texto", texto)
                .param("sha256", sha256)
                .param("quien", quien)
                .param("momento", Timestamp.from(momento))
                .query(Integer.class)
                .single();
    }

    private static LayoutGuardado fila(ResultSet fila, int numero) throws SQLException {
        return new LayoutGuardado(
                fila.getInt("numero"),
                fila.getString("texto"),
                fila.getString("sha256"),
                fila.getString("cargado_por"),
                fila.getTimestamp("cargado_en").toInstant());
    }

    private static String plantilla() {
        try (InputStream entrada = LayoutsGuardadosJdbc.class.getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                throw new IllegalStateException("Falta el recurso " + RECURSO);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
}
