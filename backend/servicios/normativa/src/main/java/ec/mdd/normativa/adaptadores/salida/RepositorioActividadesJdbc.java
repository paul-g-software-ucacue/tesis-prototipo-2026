package ec.mdd.normativa.adaptadores.salida;

import ec.mdd.normativa.dominio.ActividadDelCatalogo;
import ec.mdd.normativa.dominio.Componente;
import ec.mdd.normativa.puertos.salida.RepositorioActividades;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/** El catálogo de actividades en {@code normativa.actividad_catalogo}. */
public final class RepositorioActividadesJdbc implements RepositorioActividades {

    private static final String DE_LA_VERSION =
            "SELECT a.codigo, a.nombre, a.componente, a.tipos_de_proyecto, a.cargos"
                    + " FROM normativa.actividad_catalogo a"
                    + " JOIN normativa.version_normativa v ON v.id = a.version_id"
                    + " WHERE v.numero = :numero ORDER BY a.orden";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioActividadesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ActividadDelCatalogo> deLaVersion(int numero) {
        return jdbc.sql(DE_LA_VERSION)
                .param("numero", numero)
                .query((fila, indice) -> actividad(fila))
                .list();
    }

    private static ActividadDelCatalogo actividad(ResultSet fila) throws SQLException {
        return new ActividadDelCatalogo(
                fila.getString("codigo"),
                fila.getString("nombre"),
                Componente.valueOf(fila.getString("componente")),
                lista(fila.getArray("tipos_de_proyecto")),
                lista(fila.getArray("cargos")));
    }

    private static List<String> lista(Array arreglo) throws SQLException {
        return List.of((String[]) arreglo.getArray());
    }
}
