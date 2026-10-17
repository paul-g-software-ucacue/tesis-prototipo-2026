package ec.mdd.usuarios.adaptadores.salida;

import ec.mdd.usuarios.dominio.EstadoDeIntentos;
import ec.mdd.usuarios.puertos.salida.RepositorioIntentos;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Los intentos fallidos en {@code usuarios.intento_fallido}. Sumar un fallo es un solo {@code
 * INSERT … ON CONFLICT … DO UPDATE}: dos intentos simultáneos no pisan la cuenta (research §5).
 */
public final class RepositorioIntentosJdbc implements RepositorioIntentos {

    private static final String LEER =
            "SELECT fallidos, bloqueado_hasta FROM usuarios.intento_fallido WHERE nombre = :nombre";

    private static final String SUMAR =
            "INSERT INTO usuarios.intento_fallido (nombre, fallidos, actualizado_en)"
                    + " VALUES (:nombre, 1, :ahora)"
                    + " ON CONFLICT (nombre) DO UPDATE"
                    + " SET fallidos = intento_fallido.fallidos + 1, actualizado_en = :ahora"
                    + " RETURNING fallidos";

    private static final String BLOQUEAR =
            "UPDATE usuarios.intento_fallido SET bloqueado_hasta = :hasta, actualizado_en = :ahora"
                    + " WHERE nombre = :nombre";

    private static final String BORRAR =
            "DELETE FROM usuarios.intento_fallido WHERE nombre = :nombre";

    private static final String LIMPIAR =
            "DELETE FROM usuarios.intento_fallido WHERE actualizado_en < :desde"
                    + " AND (bloqueado_hasta IS NULL OR bloqueado_hasta <= :ahora)";

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioIntentosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public EstadoDeIntentos leer(String nombre) {
        return jdbc.sql(LEER)
                .param("nombre", nombre)
                .query(
                        (fila, numero) -> {
                            Timestamp hasta = fila.getTimestamp("bloqueado_hasta");
                            return new EstadoDeIntentos(
                                    fila.getInt("fallidos"),
                                    hasta == null ? null : hasta.toInstant());
                        })
                .optional()
                .orElse(EstadoDeIntentos.SIN_INTENTOS);
    }

    @Override
    public int sumarFallo(String nombre, Instant ahora) {
        return jdbc.sql(SUMAR)
                .param("nombre", nombre)
                .param("ahora", Timestamp.from(ahora))
                .query(Integer.class)
                .single();
    }

    @Override
    public void bloquear(String nombre, Instant hasta, Instant ahora) {
        jdbc.sql(BLOQUEAR)
                .param("nombre", nombre)
                .param("hasta", Timestamp.from(hasta))
                .param("ahora", Timestamp.from(ahora))
                .update();
    }

    @Override
    public void borrar(String nombre) {
        jdbc.sql(BORRAR).param("nombre", nombre).update();
    }

    @Override
    public int limpiar(Instant sinActividadDesde, Instant ahora) {
        return jdbc.sql(LIMPIAR)
                .param("desde", Timestamp.from(sinActividadDesde))
                .param("ahora", Timestamp.from(ahora))
                .update();
    }
}
