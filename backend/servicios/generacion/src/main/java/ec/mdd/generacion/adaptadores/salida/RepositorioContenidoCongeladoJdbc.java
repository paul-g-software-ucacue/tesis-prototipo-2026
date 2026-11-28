package ec.mdd.generacion.adaptadores.salida;

import ec.mdd.generacion.dominio.ActividadCongelada;
import ec.mdd.generacion.dominio.AsignaturaCongelada;
import ec.mdd.generacion.dominio.CargaCongelada;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.Componente;
import ec.mdd.generacion.dominio.ContenidoCongelado;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.DocenteDeLaVersion;
import ec.mdd.generacion.dominio.InconsistenciasAlCongelar;
import ec.mdd.generacion.dominio.TipoDeIdentificacion;
import ec.mdd.generacion.puertos.salida.RepositorioContenidoCongelado;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * El contenido congelado en {@code generacion.contenido_congelado} y {@code carga_congelada}
 * (F20-H01, D-115). La carga de cada docente va en {@code jsonb}, sin la identificación ni el
 * nombre, que son columnas. El JSON se arma a mano para que el dominio no dependa de Jackson.
 *
 * <p>Las horas se guardan como texto decimal, para no perder la escala de {@link BigDecimal}.
 */
public final class RepositorioContenidoCongeladoJdbc implements RepositorioContenidoCongelado {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final String GUARDAR_CABECERA =
            "INSERT INTO generacion.contenido_congelado (version_id, periodo, numero, hash,"
                    + " guardado_en, tardio, duras_abiertas, blandas_abiertas) VALUES (:version,"
                    + " :periodo, :numero, :hash, :momento, :tardio, :duras, :blandas)"
                    + " ON CONFLICT DO NOTHING";

    // F19-H02 (D-120): las dos columnas se llenan juntas o ninguna.
    private static final String INCONSISTENCIAS =
            "SELECT duras_abiertas, blandas_abiertas FROM generacion.contenido_congelado"
                    + " WHERE periodo = :periodo AND numero = :numero"
                    + " AND duras_abiertas IS NOT NULL";

    private static final String GUARDAR_CARGA =
            "INSERT INTO generacion.carga_congelada (version_id, docente, nombre, contenido)"
                    + " VALUES (:version, :docente, :nombre, CAST(:contenido AS jsonb))";

    private static final String HASH =
            "SELECT hash FROM generacion.contenido_congelado"
                    + " WHERE periodo = :periodo AND numero = :numero";

    // F20-H02 (D-117): la dedicación y las horas salen del mismo contenido que lee carga(), así
    // el reporte general y el individual no pueden decir cifras distintas.
    private static final String DOCENTES =
            "SELECT k.docente, k.nombre, k.contenido->>'dedicacion' AS dedicacion,"
                    + " k.contenido->'horas' AS horas FROM generacion.carga_congelada k"
                    + " JOIN generacion.contenido_congelado c ON c.version_id = k.version_id"
                    + " WHERE c.periodo = :periodo AND c.numero = :numero"
                    + " ORDER BY k.nombre, k.docente";

    private static final String CARGA =
            "SELECT k.docente, k.nombre, k.contenido FROM generacion.carga_congelada k"
                    + " JOIN generacion.contenido_congelado c ON c.version_id = k.version_id"
                    + " WHERE c.periodo = :periodo AND c.numero = :numero"
                    + " AND k.docente = :docente";

    private static final String CARGAS =
            "SELECT k.docente, k.nombre, k.contenido FROM generacion.carga_congelada k"
                    + " JOIN generacion.contenido_congelado c ON c.version_id = k.version_id"
                    + " WHERE c.periodo = :periodo AND c.numero = :numero"
                    + " ORDER BY k.nombre, k.docente";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioContenidoCongeladoJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // La cabecera con ON CONFLICT DO NOTHING decide quién guarda: si dos guardados llegan a la
    // vez, el segundo espera la clave del primero, no inserta nada y tampoco las cargas.
    @Override
    public boolean guardar(
            UUID version,
            ContenidoCongelado contenido,
            Optional<InconsistenciasAlCongelar> inconsistencias,
            Instant momento,
            boolean tardio) {
        int guardadas =
                jdbc.sql(GUARDAR_CABECERA)
                        .param("version", version)
                        .param("periodo", contenido.periodo())
                        .param("numero", contenido.numero())
                        .param("hash", contenido.hash())
                        .param("momento", Timestamp.from(momento))
                        .param("tardio", tardio)
                        .param(
                                "duras",
                                inconsistencias.map(InconsistenciasAlCongelar::duras).orElse(null),
                                Types.INTEGER)
                        .param(
                                "blandas",
                                inconsistencias
                                        .map(InconsistenciasAlCongelar::blandas)
                                        .orElse(null),
                                Types.INTEGER)
                        .update();
        if (guardadas == 0) {
            return false;
        }
        for (CargaCongelada carga : contenido.cargas()) {
            jdbc.sql(GUARDAR_CARGA)
                    .param("version", version)
                    .param("docente", carga.identificacion())
                    .param("nombre", carga.nombre())
                    .param("contenido", JSON.writeValueAsString(json(carga)))
                    .update();
        }
        return true;
    }

    @Override
    public Optional<InconsistenciasAlCongelar> inconsistencias(String periodo, int numero) {
        return jdbc.sql(INCONSISTENCIAS)
                .param("periodo", periodo)
                .param("numero", numero)
                .query(
                        (fila, n) ->
                                new InconsistenciasAlCongelar(
                                        fila.getInt("duras_abiertas"),
                                        fila.getInt("blandas_abiertas")))
                .optional();
    }

    @Override
    public Optional<String> hash(String periodo, int numero) {
        return jdbc.sql(HASH)
                .param("periodo", periodo)
                .param("numero", numero)
                .query(String.class)
                .optional();
    }

    @Override
    public List<DocenteDeLaVersion> docentes(String periodo, int numero) {
        return jdbc.sql(DOCENTES)
                .param("periodo", periodo)
                .param("numero", numero)
                .query(
                        (fila, n) ->
                                new DocenteDeLaVersion(
                                        fila.getString("docente"),
                                        fila.getString("nombre"),
                                        fila.getString("dedicacion") == null
                                                ? null
                                                : Dedicacion.valueOf(fila.getString("dedicacion")),
                                        horas(JSON.readTree(fila.getString("horas")))))
                .list();
    }

    @Override
    public Optional<CargaCongelada> carga(String periodo, int numero, String docente) {
        return jdbc.sql(CARGA)
                .param("periodo", periodo)
                .param("numero", numero)
                .param("docente", docente)
                .query(
                        (fila, n) ->
                                carga(
                                        fila.getString("docente"),
                                        fila.getString("nombre"),
                                        JSON.readTree(fila.getString("contenido"))))
                .optional();
    }

    @Override
    public List<CargaCongelada> cargas(String periodo, int numero) {
        return jdbc.sql(CARGAS)
                .param("periodo", periodo)
                .param("numero", numero)
                .query(
                        (fila, n) ->
                                carga(
                                        fila.getString("docente"),
                                        fila.getString("nombre"),
                                        JSON.readTree(fila.getString("contenido"))))
                .list();
    }

    private static Map<String, Object> json(CargaCongelada carga) {
        Map<String, Object> contenido = new LinkedHashMap<>();
        carga.tipoIdentificacion()
                .ifPresent(una -> contenido.put("tipoIdentificacion", una.name()));
        carga.dedicacion().ifPresent(una -> contenido.put("dedicacion", una.name()));
        carga.horasContratadas().ifPresent(horas -> contenido.put("horasContratadas", horas));
        List<Map<String, Object>> asignaturas = new ArrayList<>();
        for (AsignaturaCongelada una : carga.asignaturas()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codigo", una.codigo());
            fila.put("nombre", una.nombre());
            fila.put("paralelo", una.paralelo());
            fila.put("carrera", una.carrera());
            fila.put("nombreCarrera", una.nombreCarrera());
            fila.put("horas", una.horas().toPlainString());
            asignaturas.add(fila);
        }
        contenido.put("asignaturas", asignaturas);
        List<Map<String, Object>> actividades = new ArrayList<>();
        for (ActividadCongelada una : carga.actividades()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codigo", una.codigo());
            fila.put("nombre", una.nombre());
            fila.put("componente", una.componente().name());
            fila.put("carrera", una.carrera());
            if (una.proyecto() != null) {
                fila.put("proyecto", una.proyecto());
            }
            if (una.nombreProyecto() != null) {
                fila.put("nombreProyecto", una.nombreProyecto());
            }
            fila.put("horas", una.horas().toPlainString());
            actividades.add(fila);
        }
        contenido.put("actividades", actividades);
        Map<String, Object> horas = new LinkedHashMap<>();
        horas.put("clase", carga.horas().clase().toPlainString());
        horas.put("otrasDocencia", carga.horas().otrasDocencia().toPlainString());
        horas.put("investigacion", carga.horas().investigacion().toPlainString());
        horas.put("vinculacion", carga.horas().vinculacion().toPlainString());
        horas.put("gestion", carga.horas().gestion().toPlainString());
        contenido.put("horas", horas);
        return contenido;
    }

    private static CargaCongelada carga(String docente, String nombre, JsonNode contenido) {
        List<AsignaturaCongelada> asignaturas = new ArrayList<>();
        for (JsonNode una : contenido.get("asignaturas")) {
            asignaturas.add(
                    new AsignaturaCongelada(
                            una.get("codigo").asString(),
                            una.get("nombre").asString(),
                            una.get("paralelo").asString(),
                            una.get("carrera").asString(),
                            una.get("nombreCarrera").asString(),
                            new BigDecimal(una.get("horas").asString())));
        }
        List<ActividadCongelada> actividades = new ArrayList<>();
        for (JsonNode una : contenido.get("actividades")) {
            actividades.add(
                    new ActividadCongelada(
                            una.get("codigo").asString(),
                            una.get("nombre").asString(),
                            Componente.valueOf(una.get("componente").asString()),
                            una.get("carrera").asString(),
                            texto(una, "proyecto"),
                            texto(una, "nombreProyecto"),
                            new BigDecimal(una.get("horas").asString())));
        }
        return new CargaCongelada(
                docente,
                nombre,
                contenido.has("tipoIdentificacion")
                        ? TipoDeIdentificacion.valueOf(
                                contenido.get("tipoIdentificacion").asString())
                        : null,
                contenido.has("dedicacion")
                        ? Dedicacion.valueOf(contenido.get("dedicacion").asString())
                        : null,
                contenido.has("horasContratadas")
                        ? contenido.get("horasContratadas").asInt()
                        : null,
                asignaturas,
                actividades,
                horas(contenido.get("horas")));
    }

    private static CargaHoraria horas(JsonNode horas) {
        return new CargaHoraria(
                new BigDecimal(horas.get("clase").asString()),
                new BigDecimal(horas.get("otrasDocencia").asString()),
                new BigDecimal(horas.get("investigacion").asString()),
                new BigDecimal(horas.get("vinculacion").asString()),
                new BigDecimal(horas.get("gestion").asString()));
    }

    private static String texto(JsonNode nodo, String campo) {
        return nodo.has(campo) ? nodo.get(campo).asString() : null;
    }
}
