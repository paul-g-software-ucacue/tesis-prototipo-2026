package ec.mdd.usuarios.adaptadores.salida;

import ec.mdd.usuarios.dominio.EntradaBitacora;
import ec.mdd.usuarios.puertos.salida.RepositorioBitacora;
import java.sql.Timestamp;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * La bitácora en {@code usuarios.bitacora}, solo con {@code INSERT}. La tabla rechaza cualquier
 * {@code UPDATE}, {@code DELETE} o {@code TRUNCATE} con sus disparadores (V5).
 */
public final class RepositorioBitacoraJdbc implements RepositorioBitacora {

    private static final String REGISTRAR =
            "INSERT INTO usuarios.bitacora (id_evento, servicio, accion, usuario, momento, periodo,"
                    + " objeto, anterior, nuevo) VALUES (:id, :servicio, :accion, :usuario,"
                    + " :momento, :periodo, CAST(:objeto AS jsonb), CAST(:anterior AS jsonb),"
                    + " CAST(:nuevo AS jsonb))";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioBitacoraJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(EntradaBitacora entrada) {
        jdbc.sql(REGISTRAR)
                .param("id", entrada.idEvento())
                .param("servicio", entrada.servicio())
                .param("accion", entrada.accion())
                .param("usuario", entrada.usuario())
                .param("momento", Timestamp.from(entrada.momento()))
                .param("periodo", entrada.periodo().orElse(null))
                .param("objeto", JSON.writeValueAsString(entrada.objeto()))
                .param("anterior", json(entrada.anterior().orElse(null)))
                .param("nuevo", json(entrada.nuevo().orElse(null)))
                .update();
    }

    private static String json(Map<String, Object> valores) {
        return valores == null ? null : JSON.writeValueAsString(valores);
    }
}
