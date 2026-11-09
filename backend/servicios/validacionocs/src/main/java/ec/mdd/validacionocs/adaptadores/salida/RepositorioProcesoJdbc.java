package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.validacionocs.dominio.EstadoDeEnvio;
import ec.mdd.validacionocs.dominio.EtapaDelProceso;
import ec.mdd.validacionocs.dominio.Observacion;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los procesos de aprobación en {@code validacionocs.proceso_aprobacion}, con sus parciales y sus
 * transiciones (F12-H02, V3), y las observaciones de cada devolución (F12-H05, V4). Las
 * transiciones y sus observaciones solo se insertan (principio VII).
 */
public final class RepositorioProcesoJdbc implements RepositorioProceso {

    private static final String PROCESO =
            "SELECT periodo, estado, etapas::text AS etapas, distributivo_creado_en"
                    + " FROM validacionocs.proceso_aprobacion WHERE periodo = :periodo";

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<List<EtapaGuardada>> ETAPAS = new TypeReference<>() {};

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioProcesoJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ProcesoAprobacion> de(String periodo) {
        return leer(PROCESO, periodo);
    }

    @Override
    public Optional<ProcesoAprobacion> bloquear(String periodo) {
        return leer(PROCESO + " FOR UPDATE", periodo);
    }

    @Override
    public void crear(ProcesoAprobacion proceso, Instant ahora) {
        // Los parciales y las transiciones del anterior se van con él (on delete cascade).
        jdbc.sql("DELETE FROM validacionocs.proceso_aprobacion WHERE periodo = :periodo")
                .param("periodo", proceso.periodo())
                .update();
        jdbc.sql(
                        "INSERT INTO validacionocs.proceso_aprobacion (periodo, estado, etapas,"
                                + " distributivo_creado_en, creado_en) VALUES (:periodo, :estado,"
                                + " CAST(:etapas AS jsonb), :distributivo, :ahora)")
                .param("periodo", proceso.periodo())
                .param("estado", proceso.estado().name())
                .param("etapas", JSON.writeValueAsString(etapas(proceso.etapas())))
                .param("distributivo", Timestamp.from(proceso.distributivoCreadoEn()))
                .param("ahora", Timestamp.from(ahora))
                .update();
        guardarParciales(proceso);
        proceso.transiciones().forEach(una -> insertar(proceso.periodo(), una));
    }

    @Override
    public void guardar(ProcesoAprobacion proceso, List<Transicion> nuevas) {
        jdbc.sql(
                        "UPDATE validacionocs.proceso_aprobacion SET estado = :estado"
                                + " WHERE periodo = :periodo")
                .param("estado", proceso.estado().name())
                .param("periodo", proceso.periodo())
                .update();
        guardarParciales(proceso);
        nuevas.forEach(una -> insertar(proceso.periodo(), una));
    }

    private void guardarParciales(ProcesoAprobacion proceso) {
        proceso.parciales()
                .forEach(
                        (carrera, estado) ->
                                jdbc.sql(
                                                "INSERT INTO validacionocs.parcial_del_proceso"
                                                        + " (periodo, carrera, estado) VALUES"
                                                        + " (:periodo, :carrera, :estado)"
                                                        + " ON CONFLICT (periodo, carrera)"
                                                        + " DO UPDATE SET estado = :estado")
                                        .param("periodo", proceso.periodo())
                                        .param("carrera", carrera)
                                        .param("estado", estado.name())
                                        .update());
    }

    private void insertar(String periodo, Transicion transicion) {
        UUID id = UUID.randomUUID();
        jdbc.sql(
                        "INSERT INTO validacionocs.transicion (id, periodo, tipo, desde, hacia,"
                                + " carrera, version, hash, quien, momento) VALUES (:id, :periodo,"
                                + " :tipo, :desde, :hacia, :carrera, :version, :hash, :quien,"
                                + " :momento)")
                .param("id", id)
                .param("periodo", periodo)
                .param("tipo", transicion.tipo().name())
                .param("desde", transicion.desde().name())
                .param("hacia", transicion.hacia().name())
                .param("carrera", transicion.carrera().orElse(null))
                .param("version", transicion.version())
                .param("hash", transicion.hash())
                .param("quien", transicion.quien())
                .param("momento", Timestamp.from(transicion.momento()))
                .update();
        // Las observaciones de una devolución (F12-H05), también solo de inserción.
        transicion
                .observaciones()
                .forEach(
                        (carrera, observacion) ->
                                jdbc.sql(
                                                "INSERT INTO validacionocs.observacion_de_devolucion"
                                                        + " (transicion_id, carrera, texto) VALUES"
                                                        + " (:id, :carrera, :texto)")
                                        .param("id", id)
                                        .param("carrera", carrera)
                                        .param("texto", observacion.texto())
                                        .update());
    }

    private Optional<ProcesoAprobacion> leer(String consulta, String periodo) {
        return jdbc.sql(consulta)
                .param("periodo", periodo)
                .query(
                        (fila, numero) ->
                                new Cabecera(
                                        TipoDeEtapa.valueOf(fila.getString("estado")),
                                        fila.getString("etapas"),
                                        fila.getTimestamp("distributivo_creado_en").toInstant()))
                .optional()
                .map(
                        cabecera ->
                                ProcesoAprobacion.de(
                                        periodo,
                                        JSON.readValue(cabecera.etapas(), ETAPAS).stream()
                                                .map(EtapaGuardada::etapa)
                                                .toList(),
                                        cabecera.estado(),
                                        cabecera.distributivoCreadoEn(),
                                        parciales(periodo),
                                        transiciones(periodo)));
    }

    private Map<String, EstadoDeEnvio> parciales(String periodo) {
        Map<String, EstadoDeEnvio> parciales = new LinkedHashMap<>();
        jdbc.sql(
                        "SELECT carrera, estado FROM validacionocs.parcial_del_proceso"
                                + " WHERE periodo = :periodo ORDER BY carrera")
                .param("periodo", periodo)
                .query(
                        (fila, numero) ->
                                parciales.put(
                                        fila.getString("carrera"),
                                        EstadoDeEnvio.valueOf(fila.getString("estado"))))
                .list();
        return parciales;
    }

    private List<Transicion> transiciones(String periodo) {
        Map<UUID, Map<String, Observacion>> observaciones = observaciones(periodo);
        // El orden de inserción dentro de un mismo momento: el envío antes que su paso de etapa.
        return jdbc.sql(
                        "SELECT id, tipo, desde, hacia, carrera, version, hash, quien, momento"
                                + " FROM validacionocs.transicion WHERE periodo = :periodo"
                                + " ORDER BY momento, CASE tipo WHEN 'ENVIO_PARCIAL' THEN 0 ELSE 1"
                                + " END, ctid")
                .param("periodo", periodo)
                .query(
                        (fila, numero) ->
                                new Transicion(
                                        TipoDeTransicion.valueOf(fila.getString("tipo")),
                                        TipoDeEtapa.valueOf(fila.getString("desde")),
                                        TipoDeEtapa.valueOf(fila.getString("hacia")),
                                        Optional.ofNullable(fila.getString("carrera")),
                                        fila.getInt("version"),
                                        fila.getString("hash"),
                                        fila.getString("quien"),
                                        fila.getTimestamp("momento").toInstant(),
                                        observaciones.getOrDefault(
                                                fila.getObject("id", UUID.class), Map.of())))
                .list();
    }

    private Map<UUID, Map<String, Observacion>> observaciones(String periodo) {
        Map<UUID, Map<String, Observacion>> porTransicion = new HashMap<>();
        jdbc.sql(
                        "SELECT o.transicion_id, o.carrera, o.texto"
                                + " FROM validacionocs.observacion_de_devolucion o"
                                + " JOIN validacionocs.transicion t ON t.id = o.transicion_id"
                                + " WHERE t.periodo = :periodo")
                .param("periodo", periodo)
                .query(
                        (fila, numero) ->
                                porTransicion
                                        .computeIfAbsent(
                                                fila.getObject("transicion_id", UUID.class),
                                                id -> new TreeMap<>())
                                        .put(
                                                fila.getString("carrera"),
                                                new Observacion(fila.getString("texto"))))
                .list();
        return porTransicion;
    }

    private static List<EtapaGuardada> etapas(List<EtapaDelProceso> etapas) {
        return etapas.stream()
                .map(
                        una ->
                                new EtapaGuardada(
                                        una.tipo().name(),
                                        una.activa(),
                                        una.nombre(),
                                        una.rol(),
                                        una.admiteDevolucion()))
                .toList();
    }

    private record Cabecera(TipoDeEtapa estado, String etapas, Instant distributivoCreadoEn) {}

    /**
     * Una etapa de la copia, tal como va en el jsonb. {@code admiteDevolucion} llegó con F12-H05;
     * la migración V4 lo completó en los procesos anteriores.
     */
    record EtapaGuardada(
            String tipo, boolean activa, String nombre, String rol, boolean admiteDevolucion) {

        EtapaDelProceso etapa() {
            return new EtapaDelProceso(
                    TipoDeEtapa.valueOf(tipo), activa, nombre, rol, admiteDevolucion);
        }
    }
}
