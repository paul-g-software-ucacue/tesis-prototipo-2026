package ec.mdd.roles.puertos.entrada;

import java.util.List;

/**
 * Lo que un usuario puede hacer: sus roles y sus permisos efectivos (ADR-021). Refleja el esquema
 * {@code Autorizacion} del contrato.
 *
 * @param usuario nombre de usuario normalizado
 * @param roles identificadores de sus roles, ordenados
 * @param permisos permisos efectivos en la forma {@code FUNCIONALIDAD:TIPO[:PROPIO]}, ordenados
 * @param carrera la carrera del coordinador; {@code null} si no tiene (D-64)
 */
public record Autorizacion(
        String usuario, List<String> roles, List<String> permisos, String carrera) {

    /** Copia las listas. */
    public Autorizacion {
        roles = List.copyOf(roles);
        permisos = List.copyOf(permisos);
    }
}
