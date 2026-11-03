package ec.mdd.roles.puertos.entrada;

import java.util.List;

/**
 * Quién puede firmar en un periodo, sin decir qué usuarios (principio IX).
 *
 * @param rolesConUsuario los roles con al menos un usuario
 * @param responsableDesignado si el periodo tiene Responsable de consolidación
 */
public record Firmantes(List<String> rolesConUsuario, boolean responsableDesignado) {

    /** Copia la lista. */
    public Firmantes {
        rolesConUsuario = List.copyOf(rolesConUsuario);
    }
}
