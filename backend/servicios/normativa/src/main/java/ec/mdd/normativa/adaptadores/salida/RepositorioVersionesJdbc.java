package ec.mdd.normativa.adaptadores.salida;

import ec.mdd.normativa.dominio.AsignacionNormativa;
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
import java.sql.Timestamp;
import java.time.Instant;
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
 * política, sus reglas con sus parámetros y sus periodos, en cuatro consultas. Cada versión se
 * reconstruye con el agregado, que comprueba sus invariantes.
 *
 * <p>Las reglas se leen con {@code LEFT JOIN}, porque las del motor pueden no tener parámetros
 * (F04-H03).
 *
 * <p>Guardar una versión existente reemplaza sus filas y sus parámetros, en la transacción en
 * curso. Los candados son los de research §3: uno de la transacción para numerar y otro por
 * periodo, y {@code FOR UPDATE} sobre la versión.
 */
public final class RepositorioVersionesJdbc implements RepositorioVersiones {

    private static final String VERSIONES =
            "SELECT id, numero, codigo, origen, vigente_desde, tipo_ist, copiada_de"
                    + " FROM normativa.version_normativa";

    private static final String FILAS =
            "SELECT version_id, aplica_a, perfil, dedicacion_total, horas_clase,"
                    + " horas_clase_minimo, horas_clase_maximo, horas_otras_docencia, horas_gestion,"
                    + " horas_inv_vinc_maximo, severidad, referencia, observacion"
                    + " FROM normativa.fila_carga_horaria ORDER BY version_id, orden";

    private static final String PARAMETROS =
            "SELECT r.version_id, r.identificador, r.severidad, p.nombre, p.valor"
                    + " FROM normativa.regla r LEFT JOIN normativa.parametro_regla p"
                    + " ON p.version_id = r.version_id AND p.regla = r.identificador"
                    + " ORDER BY r.version_id, r.identificador, p.nombre";

    private static final String ASIGNACIONES =
            "SELECT version_id, periodo, asociada_en FROM normativa.asignacion_normativa"
                    + " ORDER BY version_id, asociada_en, periodo";

    private static final String ID =
            "SELECT id FROM normativa.version_normativa WHERE numero = :numero";

    // Un número cualquiera, distinto para cada candado: el de numerar y el de cada periodo.
    private static final String CANDADO = "SELECT pg_advisory_xact_lock(hashtext(:clave))";

    private static final String INSERTAR_VERSION =
            "INSERT INTO normativa.version_normativa"
                    + " (id, numero, codigo, origen, vigente_desde, tipo_ist, creada_en, copiada_de)"
                    + " VALUES (:id, :numero, :codigo, :origen, :vigente, :tipo, :creada, :copiada)";

    private static final String ACTUALIZAR_VERSION =
            "UPDATE normativa.version_normativa SET codigo = :codigo, origen = :origen,"
                    + " vigente_desde = :vigente WHERE id = :id";

    private static final String INSERTAR_FILA =
            "INSERT INTO normativa.fila_carga_horaria (version_id, orden, aplica_a, perfil,"
                    + " dedicacion_total, horas_clase, horas_clase_minimo, horas_clase_maximo,"
                    + " horas_otras_docencia, horas_gestion, horas_inv_vinc_maximo, severidad,"
                    + " referencia, observacion) VALUES (:version, :orden, :aplicaA, :perfil,"
                    + " :dedicacion, :clase, :minimo, :maximo, :otras, :gestion, :investigacion,"
                    + " :severidad, :referencia, :observacion)";

    private static final String INSERTAR_REGLA =
            "INSERT INTO normativa.regla (version_id, identificador, severidad)"
                    + " VALUES (:version, :identificador, :severidad)";

    private static final String INSERTAR_PARAMETRO =
            "INSERT INTO normativa.parametro_regla (version_id, regla, nombre, valor)"
                    + " VALUES (:version, :regla, :nombre, :valor)";

    private static final String INSERTAR_ASIGNACION =
            "INSERT INTO normativa.asignacion_normativa"
                    + " (version_id, periodo, asociada_en, asociada_por)"
                    + " SELECT :version, :periodo, :asociadaEn, :quien WHERE NOT EXISTS"
                    + " (SELECT 1 FROM normativa.asignacion_normativa"
                    + " WHERE version_id = :version AND periodo = :periodo)";

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

    @Override
    public Optional<VersionNormativa> bloquear(int numero) {
        boolean existe =
                !jdbc.sql(ID + " FOR UPDATE")
                        .param("numero", numero)
                        .query(UUID.class)
                        .list()
                        .isEmpty();
        return existe ? buscar(numero) : Optional.empty();
    }

    @Override
    public int siguienteNumero() {
        candado("normativa.version_normativa.numero");
        return jdbc.sql("SELECT coalesce(max(numero), 0) + 1 FROM normativa.version_normativa")
                .query(Integer.class)
                .single();
    }

    @Override
    public boolean existeCodigo(String codigo, int salvo) {
        return jdbc.sql(
                                "SELECT count(*) FROM normativa.version_normativa"
                                        + " WHERE codigo = :codigo AND numero <> :salvo")
                        .param("codigo", codigo)
                        .param("salvo", salvo)
                        .query(Integer.class)
                        .single()
                > 0;
    }

    @Override
    public void bloquearPeriodo(String periodo) {
        candado("normativa.asignacion_normativa." + periodo);
    }

    @Override
    public Optional<VersionNormativa> porPeriodo(String periodo) {
        return jdbc.sql(
                        "SELECT v.numero FROM normativa.asignacion_normativa a"
                                + " JOIN normativa.version_normativa v ON v.id = a.version_id"
                                + " WHERE a.periodo = :periodo")
                .param("periodo", periodo)
                .query(Integer.class)
                .optional()
                .flatMap(this::buscar);
    }

    @Override
    public void guardar(VersionNormativa version, String quien, Instant momento) {
        Optional<UUID> existente =
                jdbc.sql(ID).param("numero", version.numero()).query(UUID.class).optional();
        UUID id = existente.orElseGet(UUID::randomUUID);
        if (existente.isPresent()) {
            jdbc.sql(ACTUALIZAR_VERSION)
                    .param("id", id)
                    .param("codigo", version.codigo())
                    .param("origen", version.origen())
                    .param("vigente", version.vigenteDesde())
                    .update();
            for (String tabla : List.of("parametro_regla", "regla", "fila_carga_horaria")) {
                jdbc.sql("DELETE FROM normativa." + tabla + " WHERE version_id = :id")
                        .param("id", id)
                        .update();
            }
        } else {
            jdbc.sql(INSERTAR_VERSION)
                    .param("id", id)
                    .param("numero", version.numero())
                    .param("codigo", version.codigo())
                    .param("origen", version.origen())
                    .param("vigente", version.vigenteDesde())
                    .param("tipo", version.tipoIst().name())
                    .param("creada", Timestamp.from(momento))
                    .param("copiada", version.copiadaDe())
                    .update();
        }
        insertarFilas(id, version.tabla().filas());
        insertarReglas(id, version.reglas());
        for (AsignacionNormativa asignacion : version.asignaciones()) {
            jdbc.sql(INSERTAR_ASIGNACION)
                    .param("version", id)
                    .param("periodo", asignacion.periodo())
                    .param("asociadaEn", Timestamp.from(asignacion.asociadaEn()))
                    .param("quien", quien)
                    .update();
        }
    }

    private void candado(String clave) {
        jdbc.sql(CANDADO).param("clave", clave).query((fila, numero) -> 0).list();
    }

    private void insertarFilas(UUID id, List<FilaCargaHoraria> filas) {
        int orden = 0;
        for (FilaCargaHoraria fila : filas) {
            orden++;
            jdbc.sql(INSERTAR_FILA)
                    .param("version", id)
                    .param("orden", orden)
                    .param("aplicaA", fila.aplicaATodos() ? TODOS : fila.aplicaA().name())
                    .param("perfil", fila.perfil())
                    .param("dedicacion", fila.dedicacionTotal())
                    .param("clase", fila.horasClase())
                    .param("minimo", fila.horasClaseMinimo())
                    .param("maximo", fila.horasClaseMaximo())
                    .param("otras", fila.horasOtrasDocencia(), java.sql.Types.INTEGER)
                    .param("gestion", fila.horasGestion(), java.sql.Types.INTEGER)
                    .param(
                            "investigacion",
                            fila.horasInvestigacionVinculacionMaximo(),
                            java.sql.Types.INTEGER)
                    .param("severidad", fila.severidad().name())
                    .param("referencia", fila.referencia())
                    .param("observacion", fila.observacion(), java.sql.Types.VARCHAR)
                    .update();
        }
    }

    private void insertarReglas(UUID id, List<Regla> reglas) {
        for (Regla regla : reglas) {
            jdbc.sql(INSERTAR_REGLA)
                    .param("version", id)
                    .param("identificador", regla.identificador())
                    .param(
                            "severidad",
                            regla.severidad() == null ? null : regla.severidad().name(),
                            java.sql.Types.VARCHAR)
                    .update();
            regla.parametros()
                    .forEach(
                            (nombre, valor) ->
                                    jdbc.sql(INSERTAR_PARAMETRO)
                                            .param("version", id)
                                            .param("regla", regla.identificador())
                                            .param("nombre", nombre)
                                            .param("valor", valor)
                                            .update());
        }
    }

    // Las filas y las reglas de todas las versiones: nunca serán muchas.
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
                                    Map<String, BigDecimal> suyos =
                                            valores.computeIfAbsent(
                                                            version, clave -> new LinkedHashMap<>())
                                                    .computeIfAbsent(
                                                            regla, clave -> new LinkedHashMap<>());
                                    // Sin parámetros, el LEFT JOIN trae el nombre en null.
                                    String nombre = fila.getString("nombre");
                                    if (nombre != null) {
                                        suyos.put(nombre, fila.getBigDecimal("valor"));
                                    }
                                });
        Map<UUID, List<AsignacionNormativa>> asignaciones = new LinkedHashMap<>();
        jdbc.sql(ASIGNACIONES)
                .query(
                        (RowCallbackHandler)
                                fila ->
                                        asignaciones
                                                .computeIfAbsent(
                                                        fila.getObject("version_id", UUID.class),
                                                        clave -> new ArrayList<>())
                                                .add(
                                                        new AsignacionNormativa(
                                                                fila.getString("periodo"),
                                                                fila.getTimestamp("asociada_en")
                                                                        .toInstant())));
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
                                    reglas,
                                    fila.getObject("copiada_de", Integer.class),
                                    asignaciones.getOrDefault(id, List.of()));
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
