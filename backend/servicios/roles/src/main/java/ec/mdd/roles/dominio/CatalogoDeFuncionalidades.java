package ec.mdd.roles.dominio;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Las funcionalidades del MDD, en el orden en que se muestran (D-65). Son fijas: las cambia una
 * migración, no el administrador.
 */
public final class CatalogoDeFuncionalidades {

    private final Map<String, Funcionalidad> porCodigo = new LinkedHashMap<>();

    /**
     * @param funcionalidades las funcionalidades, en su orden
     */
    public CatalogoDeFuncionalidades(List<Funcionalidad> funcionalidades) {
        funcionalidades.forEach(
                funcionalidad -> porCodigo.put(funcionalidad.codigo(), funcionalidad));
    }

    /** Las funcionalidades, en su orden. */
    public List<Funcionalidad> funcionalidades() {
        return List.copyOf(porCodigo.values());
    }

    /**
     * Comprueba que cada permiso sea de una funcionalidad que existe, de un tipo que esa
     * funcionalidad admite, y que no se repita.
     *
     * @throws PermisosInvalidos con el primer problema, en español
     */
    public void validar(Collection<Permiso> permisos) {
        Set<String> vistos = new HashSet<>();
        for (Permiso permiso : permisos) {
            Funcionalidad funcionalidad = porCodigo.get(permiso.funcionalidad());
            if (funcionalidad == null) {
                throw new PermisosInvalidos(
                        "La funcionalidad " + permiso.funcionalidad() + " no existe.");
            }
            if (!funcionalidad.admite(permiso.tipo())) {
                throw new PermisosInvalidos(
                        "«"
                                + funcionalidad.nombre()
                                + "» no admite el permiso de "
                                + permiso.tipo().nombre()
                                + ".");
            }
            if (!vistos.add(permiso.funcionalidad() + ":" + permiso.tipo())) {
                throw new PermisosInvalidos(
                        "El permiso de "
                                + permiso.tipo().nombre()
                                + " de «"
                                + funcionalidad.nombre()
                                + "» está repetido.");
            }
        }
    }
}
