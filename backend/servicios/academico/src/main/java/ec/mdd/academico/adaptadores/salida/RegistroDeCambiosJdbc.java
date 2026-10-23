package ec.mdd.academico.adaptadores.salida;

import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los cambios de los periodos y de la estructura en {@code academico.registro_cambio}, con el
 * detalle en {@code jsonb}.
 */
public final class RegistroDeCambiosJdbc implements RegistroDeCambios {

    private static final String REGISTRAR =
            "INSERT INTO academico.registro_cambio (id, momento, usuario, accion, detalle)"
                    + " VALUES (:id, :momento, :usuario, :accion, CAST(:detalle AS jsonb))";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;

    /** Registro sobre el {@link JdbcClient} del proceso. */
    public RegistroDeCambiosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(CambioRegistrado cambio) {
        jdbc.sql(REGISTRAR)
                .param("id", UUID.randomUUID())
                .param("momento", Timestamp.from(cambio.momento()))
                .param("usuario", cambio.usuario())
                .param("accion", cambio.accion().name())
                .param("detalle", JSON.writeValueAsString(cambio.detalle()))
                .update();
    }
}
