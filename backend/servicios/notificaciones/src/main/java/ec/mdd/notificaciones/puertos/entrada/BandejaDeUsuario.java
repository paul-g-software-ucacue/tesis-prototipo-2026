package ec.mdd.notificaciones.puertos.entrada;

import ec.mdd.notificaciones.dominio.Notificacion;
import java.util.List;

/**
 * La bandeja de un usuario: una página de sus notificaciones y cuántas tiene sin leer en total.
 *
 * @param notificaciones de la más nueva a la más antigua
 * @param sinLeer todas las sin leer, no solo las de la página
 */
public record BandejaDeUsuario(List<Notificacion> notificaciones, int sinLeer) {

    /** Copia la lista. */
    public BandejaDeUsuario {
        notificaciones = List.copyOf(notificaciones);
    }
}
