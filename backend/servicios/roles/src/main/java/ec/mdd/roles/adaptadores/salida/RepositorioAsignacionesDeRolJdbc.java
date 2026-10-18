package ec.mdd.roles.adaptadores.salida;

import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/** Las asignaciones de rol y la marca de los roles iniciales en el esquema {@code roles}. */
public final class RepositorioAsignacionesDeRolJdbc implements RepositorioAsignacionesDeRol {

    private static final String DE_UN_USUARIO =
            "SELECT r.identificador FROM roles.asignacion_rol a"
                    + " JOIN roles.rol r ON r.id = a.rol_id"
                    + " WHERE a.nombre_usuario = :usuario ORDER BY r.identificador";

    private static final String TODAS =
            "SELECT a.nombre_usuario, r.identificador FROM roles.asignacion_rol a"
                    + " JOIN roles.rol r ON r.id = a.rol_id"
                    + " ORDER BY a.nombre_usuario, r.identificador";

    private static final String POR_ROL =
            "SELECT r.identificador, count(*) AS usuarios FROM roles.asignacion_rol a"
                    + " JOIN roles.rol r ON r.id = a.rol_id GROUP BY r.identificador";

    private static final String QUITAR =
            "DELETE FROM roles.asignacion_rol WHERE nombre_usuario = :usuario";

    private static final String ASIGNAR =
            "INSERT INTO roles.asignacion_rol (nombre_usuario, rol_id, asignado_en, asignado_por)"
                    + " SELECT :usuario, id, :momento, :quien FROM roles.rol"
                    + " WHERE identificador = :rol";

    private static final String SIN_REPETIR = " ON CONFLICT (nombre_usuario, rol_id) DO NOTHING";

    /** Quién hace los cambios que no hace una persona, como los roles iniciales. */
    public static final String SISTEMA = "sistema";

    private static final String INICIALIZAR =
            "INSERT INTO roles.inicializacion (nombre_usuario, inicializado_en)"
                    + " VALUES (:usuario, :momento) ON CONFLICT (nombre_usuario) DO NOTHING";

    private final JdbcClient jdbc;
    private final TransactionOperations transacciones;

    /**
     * @param jdbc el {@link JdbcClient} del proceso
     * @param transacciones para reemplazar los roles de un usuario en una sola transacción
     */
    public RepositorioAsignacionesDeRolJdbc(JdbcClient jdbc, TransactionOperations transacciones) {
        this.jdbc = jdbc;
        this.transacciones = transacciones;
    }

    @Override
    public List<String> rolesDe(String usuario) {
        return jdbc.sql(DE_UN_USUARIO).param("usuario", usuario).query(String.class).list();
    }

    @Override
    public Map<String, List<String>> todas() {
        Map<String, List<String>> todas = new LinkedHashMap<>();
        jdbc.sql(TODAS)
                .query(
                        (RowCallbackHandler)
                                fila ->
                                        todas.computeIfAbsent(
                                                        fila.getString("nombre_usuario"),
                                                        clave -> new ArrayList<>())
                                                .add(fila.getString("identificador")));
        return todas;
    }

    @Override
    public Map<String, Integer> usuariosPorRol() {
        Map<String, Integer> cuenta = new LinkedHashMap<>();
        jdbc.sql(POR_ROL)
                .query(
                        (RowCallbackHandler)
                                fila ->
                                        cuenta.put(
                                                fila.getString("identificador"),
                                                fila.getInt("usuarios")));
        return cuenta;
    }

    @Override
    public void reemplazar(
            String usuario, Collection<String> roles, String quien, Instant momento) {
        transacciones.executeWithoutResult(
                estado -> {
                    jdbc.sql(QUITAR).param("usuario", usuario).update();
                    for (String rol : roles) {
                        jdbc.sql(ASIGNAR)
                                .param("usuario", usuario)
                                .param("momento", Timestamp.from(momento))
                                .param("quien", quien)
                                .param("rol", rol)
                                .update();
                    }
                });
    }

    @Override
    public boolean inicializar(String usuario, Collection<String> roles, Instant momento) {
        Boolean nuevo =
                transacciones.execute(
                        estado -> {
                            int marcado =
                                    jdbc.sql(INICIALIZAR)
                                            .param("usuario", usuario)
                                            .param("momento", Timestamp.from(momento))
                                            .update();
                            if (marcado == 0) {
                                return false;
                            }
                            for (String rol : roles) {
                                jdbc.sql(ASIGNAR + SIN_REPETIR)
                                        .param("usuario", usuario)
                                        .param("momento", Timestamp.from(momento))
                                        .param("quien", SISTEMA)
                                        .param("rol", rol)
                                        .update();
                            }
                            return true;
                        });
        return Boolean.TRUE.equals(nuevo);
    }
}
