package ec.mdd.generacion.adaptadores.salida;

import ec.mdd.generacion.dominio.Consolidacion;
import ec.mdd.generacion.dominio.InformeSustentado;
import ec.mdd.generacion.puertos.salida.RepositorioConsolidacion;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Las consolidaciones y los informes, en el esquema de Generación. Solo se insertan. */
public final class RepositorioConsolidacionJdbc implements RepositorioConsolidacion {

    private static final String ULTIMA_CONSOLIDACION =
            "SELECT consolidada_por, consolidada_en FROM generacion.consolidacion"
                    + " WHERE version_id = :version ORDER BY consolidada_en DESC, id LIMIT 1";

    private static final String GUARDAR_CONSOLIDACION =
            "INSERT INTO generacion.consolidacion"
                    + " (id, version_id, consolidada_por, consolidada_en)"
                    + " VALUES (:id, :version, :por, :en)";

    private static final String ULTIMO_INFORME =
            "SELECT nombre, tamano, huella, clave, adjuntado_por, adjuntado_en"
                    + " FROM generacion.informe_sustentado"
                    + " WHERE version_id = :version ORDER BY adjuntado_en DESC, id LIMIT 1";

    private static final String GUARDAR_INFORME =
            "INSERT INTO generacion.informe_sustentado"
                    + " (id, version_id, nombre, tamano, huella, clave, adjuntado_por, adjuntado_en)"
                    + " VALUES (:id, :version, :nombre, :tamano, :huella, :clave, :por, :en)";

    private static final String ULTIMO_CAMBIO =
            "SELECT momento FROM generacion.registro_cambio"
                    + " WHERE detalle ->> 'periodo' = :periodo AND accion IN ('PROPUESTA_GUARDADA',"
                    + " 'PROPUESTA_QUITADA', 'ACTIVIDAD_GUARDADA', 'ACTIVIDAD_QUITADA')"
                    + " ORDER BY momento DESC LIMIT 1";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioConsolidacionJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Consolidacion> ultimaConsolidacion(UUID version) {
        return jdbc.sql(ULTIMA_CONSOLIDACION)
                .param("version", version)
                .query(
                        (fila, indice) ->
                                new Consolidacion(
                                        fila.getString("consolidada_por"),
                                        fila.getTimestamp("consolidada_en").toInstant()))
                .optional();
    }

    @Override
    public void guardarConsolidacion(UUID version, Consolidacion consolidacion) {
        jdbc.sql(GUARDAR_CONSOLIDACION)
                .param("id", UUID.randomUUID())
                .param("version", version)
                .param("por", consolidacion.por())
                .param("en", Timestamp.from(consolidacion.en()))
                .update();
    }

    @Override
    public Optional<InformeSustentado> ultimoInforme(UUID version) {
        return jdbc.sql(ULTIMO_INFORME)
                .param("version", version)
                .query(
                        (fila, indice) ->
                                new InformeSustentado(
                                        fila.getString("nombre"),
                                        fila.getInt("tamano"),
                                        fila.getString("huella"),
                                        fila.getString("clave"),
                                        fila.getString("adjuntado_por"),
                                        fila.getTimestamp("adjuntado_en").toInstant()))
                .optional();
    }

    @Override
    public void guardarInforme(UUID version, InformeSustentado informe) {
        jdbc.sql(GUARDAR_INFORME)
                .param("id", UUID.randomUUID())
                .param("version", version)
                .param("nombre", informe.nombre())
                .param("tamano", informe.tamano())
                .param("huella", informe.huella())
                .param("clave", informe.clave())
                .param("por", informe.por())
                .param("en", Timestamp.from(informe.en()))
                .update();
    }

    @Override
    public Optional<Instant> ultimoCambioDeAsignaciones(String periodo) {
        return jdbc.sql(ULTIMO_CAMBIO)
                .param("periodo", periodo)
                .query(Timestamp.class)
                .optional()
                .map(Timestamp::toInstant);
    }
}
