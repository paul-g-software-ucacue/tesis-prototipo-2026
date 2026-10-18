package ec.mdd.roles.puertos.entrada;

import ec.mdd.roles.dominio.Permiso;
import java.util.List;

/**
 * Lo que el administrador escribe al crear o editar un rol (esquema {@code RolEditable}).
 *
 * @param nombre nombre del rol
 * @param descripcion descripción
 * @param permisos sus permisos
 */
public record DatosDeRol(String nombre, String descripcion, List<Permiso> permisos) {

    /** Copia los permisos. */
    public DatosDeRol {
        permisos = List.copyOf(permisos);
    }
}
