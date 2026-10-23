package ec.mdd.academico.adaptadores.salida;

import ec.mdd.academico.dominio.GestionAutorizada;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * La estructura orgánica en {@code academico.unidad_organica} y el catálogo en {@code
 * academico.gestion_autorizada}, con {@link JdbcClient}. Un código repetido no se guarda: lo decide
 * la clave primaria, sin una consulta previa que otra transacción podría adelantar.
 */
public final class RepositorioEstructuraJdbc implements RepositorioEstructura {

    private static final String TODAS =
            "SELECT codigo, nombre, tipo, padre, gestion_autorizada, comision_permanente, activa"
                    + " FROM academico.unidad_organica ORDER BY codigo";

    private static final String CATALOGO =
            "SELECT codigo, nombre FROM academico.gestion_autorizada ORDER BY codigo";

    // Choca consigo mismo y con las escrituras, pero no con las lecturas.
    private static final String BLOQUEAR =
            "LOCK TABLE academico.unidad_organica IN SHARE ROW EXCLUSIVE MODE";

    private static final String REGISTRAR =
            "INSERT INTO academico.unidad_organica (codigo, nombre, tipo, padre, gestion_autorizada,"
                    + " comision_permanente, activa, registrada_en, registrada_por) VALUES (:codigo,"
                    + " :nombre, :tipo, :padre, :gestion, :permanente, :activa, :momento, :quien)"
                    + " ON CONFLICT (codigo) DO NOTHING";

    private static final String GUARDAR =
            "UPDATE academico.unidad_organica SET nombre = :nombre, padre = :padre,"
                    + " gestion_autorizada = :gestion, comision_permanente = :permanente, activa ="
                    + " :activa WHERE codigo = :codigo";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioEstructuraJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UnidadOrganica> todas() {
        return jdbc.sql(TODAS).query(RepositorioEstructuraJdbc::unidad).list();
    }

    @Override
    public List<GestionAutorizada> catalogo() {
        return jdbc.sql(CATALOGO)
                .query(
                        (fila, numero) ->
                                new GestionAutorizada(
                                        fila.getString("codigo"), fila.getString("nombre")))
                .list();
    }

    @Override
    public void bloquear() {
        jdbc.sql(BLOQUEAR).update();
    }

    @Override
    public boolean registrar(UnidadOrganica unidad, String quien, Instant momento) {
        return jdbc.sql(REGISTRAR)
                        .param("codigo", unidad.codigo())
                        .param("nombre", unidad.nombre())
                        .param("tipo", unidad.tipo().name())
                        .param("padre", unidad.padre())
                        .param("gestion", unidad.gestionAutorizada())
                        .param("permanente", unidad.comisionPermanente())
                        .param("activa", unidad.activa())
                        .param("momento", Timestamp.from(momento))
                        .param("quien", quien)
                        .update()
                == 1;
    }

    @Override
    public void guardar(UnidadOrganica unidad) {
        jdbc.sql(GUARDAR)
                .param("codigo", unidad.codigo())
                .param("nombre", unidad.nombre())
                .param("padre", unidad.padre())
                .param("gestion", unidad.gestionAutorizada())
                .param("permanente", unidad.comisionPermanente())
                .param("activa", unidad.activa())
                .update();
    }

    private static UnidadOrganica unidad(ResultSet fila, int numero) throws SQLException {
        return new UnidadOrganica(
                fila.getString("codigo"),
                fila.getString("nombre"),
                TipoDeUnidad.valueOf(fila.getString("tipo")),
                fila.getString("padre"),
                fila.getString("gestion_autorizada"),
                fila.getObject("comision_permanente", Boolean.class),
                fila.getBoolean("activa"));
    }
}
