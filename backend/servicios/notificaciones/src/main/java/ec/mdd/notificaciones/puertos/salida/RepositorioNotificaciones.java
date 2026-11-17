package ec.mdd.notificaciones.puertos.salida;

import ec.mdd.notificaciones.dominio.Notificacion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Las bandejas (F16-H01): las notificaciones solo se insertan y se marcan como leídas. */
public interface RepositorioNotificaciones {

    /**
     * Guarda las notificaciones; una que ya existe para el mismo evento, destinatario y carrera no
     * se repite (FR-012).
     *
     * @param notificaciones las notificaciones nuevas
     */
    void guardar(List<Notificacion> notificaciones);

    /**
     * @param destinatario el usuario de la bandeja
     * @param limite cuántas, de 1 a 100
     * @return sus notificaciones, de la más nueva a la más antigua
     */
    List<Notificacion> bandeja(String destinatario, int limite);

    /**
     * @param destinatario el usuario de la bandeja
     * @return cuántas tiene sin leer
     */
    int sinLeer(String destinatario);

    /**
     * @param id la notificación
     * @param destinatario el usuario de la bandeja
     * @return la notificación, si es de ese usuario
     */
    Optional<Notificacion> de(UUID id, String destinatario);

    /**
     * Marca la notificación como leída, solo si no lo estaba.
     *
     * @param id la notificación
     * @param momento cuándo
     */
    void marcarLeida(UUID id, Instant momento);
}
