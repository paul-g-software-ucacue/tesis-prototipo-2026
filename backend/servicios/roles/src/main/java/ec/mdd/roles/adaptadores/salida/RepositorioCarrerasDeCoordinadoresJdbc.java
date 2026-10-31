package ec.mdd.roles.adaptadores.salida;

import ec.mdd.roles.puertos.salida.RepositorioCarrerasDeCoordinadores;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;

/** La carrera de cada coordinador en el esquema {@code roles}. */
public final class RepositorioCarrerasDeCoordinadoresJdbc
        implements RepositorioCarrerasDeCoordinadores {

    private static final String TODAS =
            "SELECT nombre_usuario, carrera FROM roles.carrera_de_coordinador"
                    + " ORDER BY nombre_usuario";

    private static final String DE_UN_USUARIO =
            "SELECT carrera FROM roles.carrera_de_coordinador WHERE nombre_usuario = :usuario";

    private static final String GUARDAR =
            "INSERT INTO roles.carrera_de_coordinador"
                    + " (nombre_usuario, carrera, indicada_en, indicada_por)"
                    + " VALUES (:usuario, :carrera, :momento, :quien)"
                    + " ON CONFLICT (nombre_usuario) DO UPDATE SET carrera = EXCLUDED.carrera,"
                    + " indicada_en = EXCLUDED.indicada_en, indicada_por = EXCLUDED.indicada_por";

    private static final String QUITAR =
            "DELETE FROM roles.carrera_de_coordinador WHERE nombre_usuario = :usuario";

    private final JdbcClient jdbc;

    /** Recibe el {@link JdbcClient} del proceso. */
    public RepositorioCarrerasDeCoordinadoresJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, String> todas() {
        Map<String, String> carreras = new LinkedHashMap<>();
        RowCallbackHandler fila =
                resultado ->
                        carreras.put(
                                resultado.getString("nombre_usuario"),
                                resultado.getString("carrera"));
        jdbc.sql(TODAS).query(fila);
        return carreras;
    }

    @Override
    public Optional<String> de(String usuario) {
        return jdbc.sql(DE_UN_USUARIO).param("usuario", usuario).query(String.class).optional();
    }

    @Override
    public void guardar(String usuario, String carrera, String quien, Instant momento) {
        jdbc.sql(GUARDAR)
                .param("usuario", usuario)
                .param("carrera", carrera)
                .param("quien", quien)
                .param("momento", Timestamp.from(momento))
                .update();
    }

    @Override
    public boolean quitar(String usuario) {
        return jdbc.sql(QUITAR).param("usuario", usuario).update() > 0;
    }
}
