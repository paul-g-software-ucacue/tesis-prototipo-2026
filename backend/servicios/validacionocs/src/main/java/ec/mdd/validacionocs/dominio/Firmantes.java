package ec.mdd.validacionocs.dominio;

import java.util.Set;

/**
 * Quién puede firmar en un periodo, según Roles.
 *
 * @param rolesConUsuario los roles con al menos un usuario
 * @param responsableDesignado si el periodo tiene Responsable de consolidación
 */
public record Firmantes(Set<String> rolesConUsuario, boolean responsableDesignado) {

    /** Copia los roles. */
    public Firmantes {
        rolesConUsuario = Set.copyOf(rolesConUsuario);
    }
}
