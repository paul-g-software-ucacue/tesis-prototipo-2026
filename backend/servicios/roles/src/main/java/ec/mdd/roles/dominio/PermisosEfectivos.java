package ec.mdd.roles.dominio;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La unión de los permisos de todos los roles de un usuario (glosario, RF-03, FR-005). Si dos roles
 * dan el mismo permiso con alcances distintos, vale el más amplio.
 *
 * @param roles identificadores de los roles, ordenados
 * @param permisos los permisos, ordenados por su forma de texto
 */
public record PermisosEfectivos(List<String> roles, List<Permiso> permisos) {

    /** Copia las listas. */
    public PermisosEfectivos {
        roles = List.copyOf(roles);
        permisos = List.copyOf(permisos);
    }

    /** Los permisos efectivos de un usuario con esos roles; sin roles, ninguno. */
    public static PermisosEfectivos de(Collection<Rol> roles) {
        Map<String, Permiso> porClave = new LinkedHashMap<>();
        for (Rol rol : roles) {
            for (Permiso permiso : rol.permisos()) {
                porClave.merge(
                        permiso.funcionalidad() + ":" + permiso.tipo(),
                        permiso,
                        (uno, otro) ->
                                new Permiso(
                                        uno.funcionalidad(),
                                        uno.tipo(),
                                        uno.alcance().masAmplio(otro.alcance())));
            }
        }
        return new PermisosEfectivos(
                roles.stream().map(Rol::identificador).sorted().distinct().toList(),
                porClave.values().stream().sorted(Comparator.comparing(Permiso::texto)).toList());
    }

    /** Los permisos en la forma del token, ordenados (ADR-021). */
    public List<String> textos() {
        return permisos.stream().map(Permiso::texto).toList();
    }

    /** Si tiene ese permiso, con cualquier alcance. */
    public boolean incluye(String funcionalidad, TipoDePermiso tipo) {
        return permisos.stream().anyMatch(permiso -> permiso.mismoQue(funcionalidad, tipo));
    }
}
