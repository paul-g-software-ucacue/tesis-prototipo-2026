package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Cada carga es una fila nueva; el número se cuenta al leer, y nunca se actualiza (D-89). */
public final class RepositorioConfiguracionJdbc implements RepositorioConfiguracion {

    private static final String INSERTAR =
            """
            insert into validacionocs.configuracion_flujo
                (id, periodo, contenido, cargada_por, cargada_en)
            values (:id, :periodo, :contenido, :por, :en)
            """;

    private static final String VIGENTE =
            """
            select contenido, cargada_por, cargada_en,
                   (select count(*) from validacionocs.configuracion_flujo
                     where periodo = :periodo) as numero
              from validacionocs.configuracion_flujo
             where periodo = :periodo
             order by cargada_en desc, id desc
             limit 1
            """;

    private static final String VIGENTE_EN =
            """
            select contenido, cargada_por, cargada_en,
                   (select count(*) from validacionocs.configuracion_flujo
                     where periodo = :periodo and cargada_en <= :instante) as numero
              from validacionocs.configuracion_flujo
             where periodo = :periodo and cargada_en <= :instante
             order by cargada_en desc, id desc
             limit 1
            """;

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioConfiguracionJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(String periodo, String contenido, String por, Instant en) {
        jdbc.sql(INSERTAR)
                .param("id", UUID.randomUUID())
                .param("periodo", periodo)
                .param("contenido", contenido)
                .param("por", por)
                .param("en", Timestamp.from(en))
                .update();
    }

    @Override
    public Optional<CargaGuardada> vigente(String periodo) {
        return jdbc.sql(VIGENTE)
                .param("periodo", periodo)
                .query(
                        (fila, indice) ->
                                new CargaGuardada(
                                        fila.getInt("numero"),
                                        fila.getString("contenido"),
                                        fila.getString("cargada_por"),
                                        fila.getTimestamp("cargada_en").toInstant()))
                .optional();
    }

    @Override
    public Optional<CargaGuardada> vigenteEn(String periodo, Instant instante) {
        return jdbc.sql(VIGENTE_EN)
                .param("periodo", periodo)
                .param("instante", java.sql.Timestamp.from(instante))
                .query(
                        (fila, indice) ->
                                new CargaGuardada(
                                        fila.getInt("numero"),
                                        fila.getString("contenido"),
                                        fila.getString("cargada_por"),
                                        fila.getTimestamp("cargada_en").toInstant()))
                .optional();
    }
}
