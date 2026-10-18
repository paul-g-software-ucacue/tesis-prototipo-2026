package ec.mdd.roles.puertos.entrada;

import java.util.List;

/**
 * Lo que un usuario puede hacer: sus roles y sus permisos efectivos (ADR-021). Refleja el esquema
 * {@code Autorizacion} del contrato.
 *
 * @param usuario nombre de usuario normalizado
 * @param roles identificadores de sus roles, ordenados
 * @param permisos permisos efectivos en la forma {@code FUNCIONALIDAD:TIPO[:PROPIO]}, ordenados
 */
public record Autorizacion(String usuario, List<String> roles, List<String> permisos) {

    /** Copia las listas. */
    public Autorizacion {
        roles = List.copyOf(roles);
        permisos = List.copyOf(permisos);
    }
}
