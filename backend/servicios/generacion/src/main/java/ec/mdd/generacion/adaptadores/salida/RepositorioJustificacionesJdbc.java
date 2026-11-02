package ec.mdd.generacion.adaptadores.salida;

import ec.mdd.generacion.dominio.JustificacionDeInconsistencia;
import ec.mdd.generacion.dominio.TipoDeInconsistencia;
import ec.mdd.generacion.puertos.salida.RepositorioJustificaciones;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Las justificaciones, en el esquema de Generación. Solo se insertan (principio VII). */
public final class RepositorioJustificacionesJdbc implements RepositorioJustificaciones {

    private static final String VIGENTES =
            "SELECT DISTINCT ON (tipo, objeto) tipo, objeto, texto, justificada_por,"
                    + " justificada_en FROM generacion.justificacion_inconsistencia"
                    + " WHERE version_id = :version"
                    + " ORDER BY tipo, objeto, justificada_en DESC, id";

    private static final String GUARDAR =
            "INSERT INTO generacion.justificacion_inconsistencia"
                    + " (id, version_id, tipo, objeto, texto, justificada_por, justificada_en)"
                    + " VALUES (:id, :version, :tipo, :objeto, :texto, :por, :en)";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioJustificacionesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<JustificacionDeInconsistencia> vigentes(UUID version) {
        return jdbc.sql(VIGENTES)
                .param("version", version)
                .query(
                        (fila, indice) ->
                                new JustificacionDeInconsistencia(
                                        TipoDeInconsistencia.valueOf(fila.getString("tipo")),
                                        fila.getString("objeto"),
                                        fila.getString("texto"),
                                        fila.getString("justificada_por"),
                                        fila.getTimestamp("justificada_en").toInstant()))
                .list();
    }

    @Override
    public void guardar(UUID version, JustificacionDeInconsistencia justificacion) {
        jdbc.sql(GUARDAR)
                .param("id", UUID.randomUUID())
                .param("version", version)
                .param("tipo", justificacion.tipo().name())
                .param("objeto", justificacion.objeto())
                .param("texto", justificacion.texto())
                .param("por", justificacion.por())
                .param("en", Timestamp.from(justificacion.en()))
                .update();
    }
}
