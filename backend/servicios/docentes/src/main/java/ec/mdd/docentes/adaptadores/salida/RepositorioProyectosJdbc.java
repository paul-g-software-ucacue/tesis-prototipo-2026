package ec.mdd.docentes.adaptadores.salida;

import ec.mdd.docentes.dominio.EstadoDeProyecto;
import ec.mdd.docentes.dominio.Participante;
import ec.mdd.docentes.dominio.Proyecto;
import ec.mdd.docentes.dominio.RolDeParticipante;
import ec.mdd.docentes.dominio.TipoDeProyecto;
import ec.mdd.docentes.puertos.salida.RepositorioProyectos;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los proyectos en {@code docentes.proyecto}, con {@link JdbcClient}. Las carreras y los
 * participantes van en {@code jsonb}, con los nombres del contrato (F03-H05, research §6). El
 * dominio los valida antes de guardar y al leer.
 */
public final class RepositorioProyectosJdbc implements RepositorioProyectos {

    private static final String COLUMNAS =
            "codigo, nombre, tipo, carreras, resolucion_ocs, fecha_resolucion, estado,"
                    + " vigencia_inicio, vigencia_fin, director, participantes, horas_aprobadas";

    private static final String TODOS =
            "SELECT " + COLUMNAS + " FROM docentes.proyecto ORDER BY codigo";

    private static final String BUSCAR =
            "SELECT " + COLUMNAS + " FROM docentes.proyecto WHERE codigo = :codigo";

    // Choca consigo mismo y con las escrituras, pero no con las lecturas.
    private static final String BLOQUEAR =
            "LOCK TABLE docentes.proyecto IN SHARE ROW EXCLUSIVE MODE";

    private static final String REGISTRAR =
            "INSERT INTO docentes.proyecto ("
                    + COLUMNAS
                    + ", registrado_en, registrado_por) VALUES (:codigo, :nombre, :tipo,"
                    + " CAST(:carreras AS jsonb), :resolucion, :fechaResolucion, :estado, :inicio,"
                    + " :fin, :director, CAST(:participantes AS jsonb), :horas, :momento, :quien)";

    private static final String GUARDAR =
            "UPDATE docentes.proyecto SET nombre = :nombre, tipo = :tipo,"
                    + " carreras = CAST(:carreras AS jsonb), resolucion_ocs = :resolucion,"
                    + " fecha_resolucion = :fechaResolucion, estado = :estado,"
                    + " vigencia_inicio = :inicio, vigencia_fin = :fin, director = :director,"
                    + " participantes = CAST(:participantes AS jsonb), horas_aprobadas = :horas"
                    + " WHERE codigo = :codigo";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioProyectosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Proyecto> todos() {
        return jdbc.sql(TODOS).query(RepositorioProyectosJdbc::proyecto).list();
    }

    @Override
    public Optional<Proyecto> buscar(String codigo) {
        return jdbc.sql(BUSCAR)
                .param("codigo", codigo)
                .query(RepositorioProyectosJdbc::proyecto)
                .optional();
    }

    @Override
    public void bloquear() {
        jdbc.sql(BLOQUEAR).update();
    }

    @Override
    public void registrar(Proyecto proyecto, String quien, Instant momento) {
        conDatos(jdbc.sql(REGISTRAR), proyecto)
                .param("momento", Timestamp.from(momento))
                .param("quien", quien)
                .update();
    }

    @Override
    public void guardar(Proyecto proyecto) {
        conDatos(jdbc.sql(GUARDAR), proyecto).update();
    }

    private static JdbcClient.StatementSpec conDatos(
            JdbcClient.StatementSpec sentencia, Proyecto proyecto) {
        return sentencia
                .param("codigo", proyecto.codigo())
                .param("nombre", proyecto.nombre())
                .param("tipo", proyecto.tipo().name())
                .param("carreras", JSON.writeValueAsString(proyecto.carreras()))
                .param("resolucion", proyecto.resolucionOcs())
                .param("fechaResolucion", Date.valueOf(proyecto.fechaResolucion()))
                .param("estado", proyecto.estado().name())
                .param("inicio", Date.valueOf(proyecto.vigenciaInicio()))
                .param("fin", Date.valueOf(proyecto.vigenciaFin()))
                .param("director", proyecto.director())
                .param(
                        "participantes",
                        JSON.writeValueAsString(
                                proyecto.participantes().stream()
                                        .map(RepositorioProyectosJdbc::participante)
                                        .toList()))
                .param("horas", proyecto.horasAprobadas());
    }

    private static Map<String, Object> participante(Participante uno) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("docente", uno.docente());
        mapa.put("rol", uno.rol().name());
        mapa.put("horas", uno.horas());
        return mapa;
    }

    private static Proyecto proyecto(ResultSet fila, int numero) throws SQLException {
        List<String> carreras = new ArrayList<>();
        JSON.readTree(fila.getString("carreras")).forEach(nodo -> carreras.add(nodo.asString()));
        List<Participante> participantes = new ArrayList<>();
        JSON.readTree(fila.getString("participantes"))
                .forEach(
                        nodo ->
                                participantes.add(
                                        new Participante(
                                                nodo.get("docente").asString(),
                                                RolDeParticipante.valueOf(
                                                        nodo.get("rol").asString()),
                                                nodo.get("horas").decimalValue())));
        return new Proyecto(
                fila.getString("codigo"),
                fila.getString("nombre"),
                TipoDeProyecto.valueOf(fila.getString("tipo")),
                carreras,
                fila.getString("resolucion_ocs"),
                fila.getDate("fecha_resolucion").toLocalDate(),
                EstadoDeProyecto.valueOf(fila.getString("estado")),
                fila.getDate("vigencia_inicio").toLocalDate(),
                fila.getDate("vigencia_fin").toLocalDate(),
                fila.getString("director"),
                participantes,
                fila.getBigDecimal("horas_aprobadas"));
    }
}
