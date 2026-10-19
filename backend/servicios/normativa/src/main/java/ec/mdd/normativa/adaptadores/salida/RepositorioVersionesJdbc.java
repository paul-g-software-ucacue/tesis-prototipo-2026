package ec.mdd.normativa.adaptadores.salida;

import ec.mdd.normativa.dominio.FilaCargaHoraria;
import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.Severidad;
import ec.mdd.normativa.dominio.TablaCargaHoraria;
import ec.mdd.normativa.dominio.TipoIst;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Las versiones de normativa con {@link JdbcClient}: la cabecera, sus filas en el orden de la
 * política y sus reglas con sus parámetros, en tres consultas. Cada versión se reconstruye con el
 * agregado, que comprueba sus invariantes.
 */
public final class RepositorioVersionesJdbc implements RepositorioVersiones {

    private static final String VERSIONES =
            "SELECT id, numero, codigo, origen, vigente_desde, tipo_ist"
                    + " FROM normativa.version_normativa";

    private static final String FILAS =
            "SELECT version_id, aplica_a, perfil, dedicacion_total, horas_clase,"
                    + " horas_clase_minimo, horas_clase_maximo, horas_otras_docencia, horas_gestion,"
                    + " horas_inv_vinc_maximo, severidad, referencia, observacion"
                    + " FROM normativa.fila_carga_horaria ORDER BY version_id, orden";

    private static final String PARAMETROS =
            "SELECT r.version_id, r.identificador, r.severidad, p.nombre, p.valor"
                    + " FROM normativa.regla r JOIN normativa.parametro_regla p"
                    + " ON p.version_id = r.version_id AND p.regla = r.identificador"
                    + " ORDER BY r.version_id, r.identificador, p.nombre";

    private static final String TODOS = "TODOS";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioVersionesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<VersionNormativa> todas() {
        return leer(" ORDER BY numero", Map.of());
    }

    @Override
    public Optional<VersionNormativa> buscar(int numero) {
        return leer(" WHERE numero = :numero", Map.of("numero", numero)).stream().findFirst();
    }

    // Las filas y las reglas de todas las versiones: hoy hay una, y nunca serán muchas.
    private List<VersionNormativa> leer(String filtro, Map<String, ?> parametros) {
        Map<UUID, List<FilaCargaHoraria>> filas = new LinkedHashMap<>();
        jdbc.sql(FILAS)
                .query(
                        (RowCallbackHandler)
                                fila ->
                                        filas.computeIfAbsent(
                                                        fila.getObject("version_id", UUID.class),
                                                        clave -> new ArrayList<>())
                                                .add(fila(fila)));
        Map<UUID, Map<String, Severidad>> severidades = new LinkedHashMap<>();
        Map<UUID, Map<String, Map<String, BigDecimal>>> valores = new LinkedHashMap<>();
        jdbc.sql(PARAMETROS)
                .query(
                        (RowCallbackHandler)
                                fila -> {
                                    UUID version = fila.getObject("version_id", UUID.class);
                                    String regla = fila.getString("identificador");
                                    String severidad = fila.getString("severidad");
                                    severidades
                                            .computeIfAbsent(
                                                    version, clave -> new LinkedHashMap<>())
                                            .put(
                                                    regla,
                                                    severidad == null
                                                            ? null
                                                            : Severidad.valueOf(severidad));
                                    valores.computeIfAbsent(version, clave -> new LinkedHashMap<>())
                                            .computeIfAbsent(regla, clave -> new LinkedHashMap<>())
                                            .put(
                                                    fila.getString("nombre"),
                                                    fila.getBigDecimal("valor"));
                                });
        return jdbc.sql(VERSIONES + filtro)
                .params(parametros)
                .query(
                        (fila, numero) -> {
                            UUID id = fila.getObject("id", UUID.class);
                            List<Regla> reglas = new ArrayList<>();
                            valores.getOrDefault(id, Map.of())
                                    .forEach(
                                            (regla, suyos) ->
                                                    reglas.add(
                                                            new Regla(
                                                                    regla,
                                                                    severidades.get(id).get(regla),
                                                                    suyos)));
                            return new VersionNormativa(
                                    fila.getInt("numero"),
                                    fila.getString("codigo"),
                                    fila.getString("origen"),
                                    fila.getObject("vigente_desde", LocalDate.class),
                                    TipoIst.valueOf(fila.getString("tipo_ist")),
                                    new TablaCargaHoraria(filas.getOrDefault(id, List.of())),
                                    reglas);
                        })
                .list();
    }

    private static FilaCargaHoraria fila(ResultSet fila) throws SQLException {
        String aplicaA = fila.getString("aplica_a");
        return new FilaCargaHoraria(
                TODOS.equals(aplicaA) ? null : TipoIst.valueOf(aplicaA),
                fila.getString("perfil"),
                fila.getInt("dedicacion_total"),
                fila.getInt("horas_clase"),
                fila.getInt("horas_clase_minimo"),
                fila.getInt("horas_clase_maximo"),
                fila.getObject("horas_otras_docencia", Integer.class),
                fila.getObject("horas_gestion", Integer.class),
                fila.getObject("horas_inv_vinc_maximo", Integer.class),
                Severidad.valueOf(fila.getString("severidad")),
                fila.getString("referencia"),
                fila.getString("observacion"));
    }
}
