package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.validacionocs.dominio.Plazo;
import ec.mdd.validacionocs.dominio.VencimientoDePlazo;
import ec.mdd.validacionocs.puertos.salida.RepositorioPlazos;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Los vencimientos de plazo en {@code validacionocs.plazo_vencido} (F12-H03, V9, D-114). Solo se
 * insertan. La clave única {@code (periodo, etapa, llegada)} deja uno por llegada: una revisión
 * repetida, al arrancar y por el cron el mismo día, o dos a la vez, no lo duplican.
 */
public final class RepositorioPlazosJdbc implements RepositorioPlazos {

    private final JdbcClient jdbc;

    /** Repositorio sobre el {@link JdbcClient} del proceso. */
    public RepositorioPlazosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean registrar(VencimientoDePlazo vencimiento) {
        Plazo plazo = vencimiento.plazo();
        return jdbc.sql(
                                "INSERT INTO validacionocs.plazo_vencido (id, periodo, etapa,"
                                        + " llegada, plazo_dias, fecha_limite, detectado_en)"
                                        + " VALUES (:id, :periodo, :etapa, :llegada, :dias,"
                                        + " :fechaLimite, :detectadoEn)"
                                        + " ON CONFLICT (periodo, etapa, llegada) DO NOTHING")
                        .param("id", UUID.randomUUID())
                        .param("periodo", vencimiento.periodo())
                        .param("etapa", plazo.etapa().name())
                        .param("llegada", Timestamp.from(plazo.llegada()))
                        .param("dias", plazo.dias())
                        .param("fechaLimite", plazo.fechaLimite())
                        .param("detectadoEn", Timestamp.from(vencimiento.detectadoEn()))
                        .update()
                == 1;
    }
}
