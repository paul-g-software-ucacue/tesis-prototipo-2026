package ec.mdd.roles.puertos.salida;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Las asignaciones de rol a usuarios y la marca de los roles iniciales (research §1 y §8). Los
 * nombres de usuario llegan normalizados.
 */
public interface RepositorioAsignacionesDeRol {

    /** Identificadores de los roles del usuario, ordenados; vacía si no tiene. */
    List<String> rolesDe(String usuario);

    /** Los roles de cada usuario con al menos uno, ordenados por usuario y por rol. */
    Map<String, List<String>> todas();

    /** A cuántos usuarios está asignado cada rol; un rol sin usuarios no aparece. */
    Map<String, Integer> usuariosPorRol();

    /**
     * Deja al usuario exactamente con esos roles, todo junto. Los identificadores existen: los
     * comprueba la aplicación.
     *
     * @param quien el usuario que hace el cambio, para {@code asignado_por}
     */
    void reemplazar(String usuario, Collection<String> roles, String quien, Instant momento);

    /**
     * Anota que el usuario recibió sus roles iniciales.
     *
     * @return {@code true} si es la primera vez; {@code false} si ya estaba anotado
     */
    boolean marcarInicializado(String usuario, Instant momento);
}
