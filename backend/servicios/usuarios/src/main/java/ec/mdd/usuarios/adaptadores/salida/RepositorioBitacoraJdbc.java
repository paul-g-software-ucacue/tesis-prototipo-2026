package ec.mdd.usuarios.adaptadores.salida;

import ec.mdd.usuarios.dominio.EntradaBitacora;
import ec.mdd.usuarios.dominio.FiltroDeBitacora;
import ec.mdd.usuarios.dominio.PaginaPedida;
import ec.mdd.usuarios.puertos.entrada.AccionConocida;
import ec.mdd.usuarios.puertos.entrada.EntradaConsultada;
import ec.mdd.usuarios.puertos.entrada.PaginaDeEntradas;
import ec.mdd.usuarios.puertos.salida.RepositorioBitacora;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * La bitácora en {@code usuarios.bitacora}, solo con {@code INSERT} y {@code SELECT}. La tabla
 * rechaza cualquier {@code UPDATE}, {@code DELETE} o {@code TRUNCATE} con sus disparadores (V5). La
 * consulta de F17-H02 arma su condición con fragmentos fijos y pasa cada valor como parámetro.
 */
public final class RepositorioBitacoraJdbc implements RepositorioBitacora {

    private static final String REGISTRAR =
            "INSERT INTO usuarios.bitacora (id_evento, servicio, accion, usuario, momento, periodo,"
                    + " objeto, anterior, nuevo) VALUES (:id, :servicio, :accion, :usuario,"
                    + " :momento, :periodo, CAST(:objeto AS jsonb), CAST(:anterior AS jsonb),"
                    + " CAST(:nuevo AS jsonb))";

    // Un valor del objeto, a cualquier profundidad, igual al identificador: un texto igual o un
    // número con la misma representación (research §4). $id es un parámetro de jsonpath.
    private static final String IDENTIFICADOR =
            "jsonb_path_exists(objeto, '$.** ? ((@ == $id) || (@.type() == \"number\""
                    + " && @.string() == $id))', jsonb_build_object('id', CAST(:identificador AS"
                    + " text)))";

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {};

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioBitacoraJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(EntradaBitacora entrada) {
        jdbc.sql(REGISTRAR)
                .param("id", entrada.idEvento())
                .param("servicio", entrada.servicio())
                .param("accion", entrada.accion())
                .param("usuario", entrada.usuario())
                .param("momento", Timestamp.from(entrada.momento()))
                .param("periodo", entrada.periodo().orElse(null))
                .param("objeto", JSON.writeValueAsString(entrada.objeto()))
                .param("anterior", json(entrada.anterior().orElse(null)))
                .param("nuevo", json(entrada.nuevo().orElse(null)))
                .update();
    }

    @Override
    public PaginaDeEntradas consultar(FiltroDeBitacora filtro, PaginaPedida pagina) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        String donde = donde(filtro, pagina, parametros);
        long total =
                jdbc.sql("SELECT count(*) FROM usuarios.bitacora WHERE " + donde)
                        .params(parametros)
                        .query(Long.class)
                        .single();
        parametros.put("tamano", pagina.tamano());
        parametros.put("desplazamiento", pagina.desplazamiento());
        List<EntradaConsultada> entradas =
                jdbc.sql(
                                "SELECT id_evento, servicio, accion, usuario, momento, periodo,"
                                        + " objeto::text AS objeto, anterior::text AS anterior,"
                                        + " nuevo::text AS nuevo FROM usuarios.bitacora WHERE "
                                        + donde
                                        + " ORDER BY momento DESC, id_evento DESC"
                                        + " LIMIT :tamano OFFSET :desplazamiento")
                        .params(parametros)
                        .query(
                                (fila, numero) ->
                                        new EntradaConsultada(
                                                fila.getObject("id_evento", UUID.class),
                                                fila.getString("servicio"),
                                                fila.getString("usuario"),
                                                fila.getString("accion"),
                                                fila.getTimestamp("momento").toInstant(),
                                                Optional.ofNullable(fila.getString("periodo")),
                                                mapa(fila.getString("objeto")),
                                                Optional.ofNullable(fila.getString("anterior"))
                                                        .map(RepositorioBitacoraJdbc::mapa),
                                                Optional.ofNullable(fila.getString("nuevo"))
                                                        .map(RepositorioBitacoraJdbc::mapa)))
                        .list();
        return new PaginaDeEntradas(
                entradas, total, pagina.pagina(), pagina.tamano(), pagina.corte());
    }

    @Override
    public List<AccionConocida> acciones() {
        return jdbc.sql(
                        "SELECT DISTINCT servicio, accion FROM usuarios.bitacora"
                                + " ORDER BY servicio, accion")
                .query(
                        (fila, numero) ->
                                new AccionConocida(
                                        fila.getString("servicio"), fila.getString("accion")))
                .list();
    }

    // Solo fragmentos fijos: cada valor del filtro va como parámetro, nunca en el texto (OWASP
    // A03).
    private static String donde(
            FiltroDeBitacora filtro, PaginaPedida pagina, Map<String, Object> parametros) {
        List<String> condiciones = new ArrayList<>();
        condiciones.add("recibida_en <= :corte");
        parametros.put("corte", Timestamp.from(pagina.corte()));
        filtro.desde()
                .ifPresent(
                        desde -> {
                            condiciones.add("momento >= :desde");
                            parametros.put("desde", Timestamp.from(desde));
                        });
        filtro.hasta()
                .ifPresent(
                        hasta -> {
                            condiciones.add("momento < :hasta");
                            parametros.put("hasta", Timestamp.from(hasta));
                        });
        igual(filtro.usuario(), "usuario", condiciones, parametros);
        igual(filtro.servicio(), "servicio", condiciones, parametros);
        igual(filtro.accion(), "accion", condiciones, parametros);
        igual(filtro.periodo(), "periodo", condiciones, parametros);
        filtro.identificador()
                .ifPresent(
                        identificador -> {
                            condiciones.add(IDENTIFICADOR);
                            parametros.put("identificador", identificador);
                        });
        return String.join(" AND ", condiciones);
    }

    private static void igual(
            Optional<String> valor,
            String columna,
            List<String> condiciones,
            Map<String, Object> parametros) {
        valor.ifPresent(
                texto -> {
                    condiciones.add(columna + " = :" + columna);
                    parametros.put(columna, texto);
                });
    }

    private static Map<String, Object> mapa(String json) {
        return JSON.readValue(json, MAPA);
    }

    private static String json(Map<String, Object> valores) {
        return valores == null ? null : JSON.writeValueAsString(valores);
    }
}
