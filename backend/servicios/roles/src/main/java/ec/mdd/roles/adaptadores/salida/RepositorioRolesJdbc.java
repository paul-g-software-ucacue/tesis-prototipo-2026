package ec.mdd.roles.adaptadores.salida;

import ec.mdd.roles.dominio.Alcance;
import ec.mdd.roles.dominio.CatalogoDeFuncionalidades;
import ec.mdd.roles.dominio.Funcionalidad;
import ec.mdd.roles.dominio.Permiso;
import ec.mdd.roles.dominio.Rol;
import ec.mdd.roles.dominio.TipoDePermiso;
import ec.mdd.roles.puertos.salida.RepositorioRoles;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/** Las funcionalidades y los roles en el esquema {@code roles}, con {@link JdbcClient}. */
public final class RepositorioRolesJdbc implements RepositorioRoles {

    private static final String FUNCIONALIDADES =
            "SELECT f.codigo, f.nombre, ft.tipo FROM roles.funcionalidad f"
                    + " JOIN roles.funcionalidad_tipo ft ON ft.funcionalidad = f.codigo"
                    + " ORDER BY f.orden";

    private static final String ROLES =
            "SELECT id, identificador, nombre, descripcion, predefinido FROM roles.rol";

    private static final String PERMISOS =
            "SELECT rp.rol_id, rp.funcionalidad, rp.tipo, rp.alcance FROM roles.rol_permiso rp";

    private static final String GUARDAR =
            "INSERT INTO roles.rol (id, identificador, nombre, descripcion, predefinido, creado_en,"
                    + " actualizado_en)"
                    + " VALUES (:id, :identificador, :nombre, :descripcion, :predefinido, :momento,"
                    + " :momento)"
                    + " ON CONFLICT (identificador) DO UPDATE SET nombre = EXCLUDED.nombre,"
                    + " descripcion = EXCLUDED.descripcion, actualizado_en = EXCLUDED.actualizado_en"
                    + " RETURNING id";

    private static final String BORRAR_PERMISOS =
            "DELETE FROM roles.rol_permiso WHERE rol_id = :id";

    private static final String AGREGAR_PERMISO =
            "INSERT INTO roles.rol_permiso (rol_id, funcionalidad, tipo, alcance)"
                    + " VALUES (:id, :funcionalidad, :tipo, :alcance)";

    private static final String BORRAR =
            "DELETE FROM roles.rol WHERE identificador = :identificador";

    private final JdbcClient jdbc;
    private final TransactionOperations transacciones;

    /**
     * @param jdbc el {@link JdbcClient} del proceso
     * @param transacciones para guardar un rol con sus permisos en una sola transacción
     */
    public RepositorioRolesJdbc(JdbcClient jdbc, TransactionOperations transacciones) {
        this.jdbc = jdbc;
        this.transacciones = transacciones;
    }

    @Override
    public CatalogoDeFuncionalidades funcionalidades() {
        Map<String, String> nombres = new LinkedHashMap<>();
        Map<String, Set<TipoDePermiso>> tipos = new LinkedHashMap<>();
        jdbc.sql(FUNCIONALIDADES)
                .query(
                        fila -> {
                            String codigo = fila.getString("codigo");
                            nombres.put(codigo, fila.getString("nombre"));
                            tipos.computeIfAbsent(
                                            codigo, clave -> EnumSet.noneOf(TipoDePermiso.class))
                                    .add(TipoDePermiso.valueOf(fila.getString("tipo")));
                        });
        List<Funcionalidad> funcionalidades = new ArrayList<>();
        nombres.forEach(
                (codigo, nombre) ->
                        funcionalidades.add(new Funcionalidad(codigo, nombre, tipos.get(codigo))));
        return new CatalogoDeFuncionalidades(funcionalidades);
    }

    @Override
    public List<Rol> todos() {
        return leer(" ORDER BY nombre", "", Map.of());
    }

    @Override
    public Optional<Rol> buscar(String identificador) {
        return leer(
                        " WHERE identificador = :identificador",
                        " JOIN roles.rol r ON r.id = rp.rol_id WHERE r.identificador = :identificador",
                        Map.of("identificador", identificador))
                .stream()
                .findFirst();
    }

    @Override
    public void guardar(Rol rol, Instant momento) {
        transacciones.executeWithoutResult(
                estado -> {
                    UUID id =
                            jdbc.sql(GUARDAR)
                                    .param("id", UUID.randomUUID())
                                    .param("identificador", rol.identificador())
                                    .param("nombre", rol.nombre())
                                    .param("descripcion", rol.descripcion())
                                    .param("predefinido", rol.predefinido())
                                    .param("momento", Timestamp.from(momento))
                                    .query(UUID.class)
                                    .single();
                    jdbc.sql(BORRAR_PERMISOS).param("id", id).update();
                    for (Permiso permiso : rol.permisos()) {
                        jdbc.sql(AGREGAR_PERMISO)
                                .param("id", id)
                                .param("funcionalidad", permiso.funcionalidad())
                                .param("tipo", permiso.tipo().name())
                                .param("alcance", permiso.alcance().name())
                                .update();
                    }
                });
    }

    @Override
    public void borrar(String identificador) {
        jdbc.sql(BORRAR).param("identificador", identificador).update();
    }

    // Los roles con sus permisos en dos consultas, en lugar de una por rol.
    private List<Rol> leer(String filtroRoles, String filtroPermisos, Map<String, ?> parametros) {
        Map<UUID, List<Permiso>> permisos = new LinkedHashMap<>();
        jdbc.sql(PERMISOS + filtroPermisos)
                .params(parametros)
                .query(
                        (RowCallbackHandler)
                                fila ->
                                        permisos.computeIfAbsent(
                                                        fila.getObject("rol_id", UUID.class),
                                                        clave -> new ArrayList<>())
                                                .add(
                                                        new Permiso(
                                                                fila.getString("funcionalidad"),
                                                                TipoDePermiso.valueOf(
                                                                        fila.getString("tipo")),
                                                                Alcance.valueOf(
                                                                        fila.getString(
                                                                                "alcance")))));
        return jdbc.sql(ROLES + filtroRoles)
                .params(parametros)
                .query(
                        (fila, numero) ->
                                new Rol(
                                        fila.getString("identificador"),
                                        fila.getString("nombre"),
                                        fila.getString("descripcion"),
                                        fila.getBoolean("predefinido"),
                                        permisos.getOrDefault(
                                                fila.getObject("id", UUID.class), List.of())))
                .list();
    }
}
