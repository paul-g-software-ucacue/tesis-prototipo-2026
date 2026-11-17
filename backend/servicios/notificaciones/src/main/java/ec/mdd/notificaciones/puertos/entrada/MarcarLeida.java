package ec.mdd.notificaciones.puertos.entrada;

import ec.mdd.notificaciones.dominio.Notificacion;
import java.util.UUID;

/** Caso de uso: abrir una notificación la marca como leída (F16-H01, FR-008). */
@FunctionalInterface
public interface MarcarLeida {

    /**
     * Marcar una ya leída no cambia nada.
     *
     * @param id la notificación
     * @param usuario el usuario de la sesión
     * @return la notificación, leída
     * @throws ec.mdd.notificaciones.dominio.NotificacionNoEncontrada si no existe o es de otro
     *     usuario
     */
    Notificacion marcar(UUID id, String usuario);
}
