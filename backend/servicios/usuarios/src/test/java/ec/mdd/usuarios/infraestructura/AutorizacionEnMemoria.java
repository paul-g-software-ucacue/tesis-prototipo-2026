package ec.mdd.usuarios.infraestructura;

import ec.mdd.usuarios.puertos.salida.AutorizacionDeUsuario;
import ec.mdd.usuarios.puertos.salida.ProveedorDeAutorizacion;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Doble de {@link ProveedorDeAutorizacion} que se comporta como Roles: conoce los 10 roles de D-16,
 * da los roles iniciales solo la primera vez y rechaza un rol desconocido diciendo cuál.
 */
public final class AutorizacionEnMemoria implements ProveedorDeAutorizacion {

    private static final Set<String> CONOCIDOS =
            Set.of(
                    "ADMINISTRADOR",
                    "OPERADOR_DATOS",
                    "COORDINADOR_CARRERA",
                    "GESTOR_UNIDAD",
                    "RESPONSABLE_CONSOLIDACION",
                    "VICERRECTOR",
                    "RECTOR",
                    "MIEMBRO_OCS",
                    "SECRETARIA_GENERAL",
                    "DOCENTE");

    /** Los roles de cada usuario inicializado. */
    public final Map<String, List<String>> roles = new LinkedHashMap<>();

    /** Cuántas veces se pidieron roles iniciales, contando las que no hicieron nada. */
    public int pedidos;

    @Override
    public AutorizacionDeUsuario consultar(String nombreUsuario) {
        return new AutorizacionDeUsuario(roles.getOrDefault(nombreUsuario, List.of()), List.of());
    }

    @Override
    public void asignarRolesIniciales(String nombreUsuario, Collection<String> iniciales) {
        pedidos++;
        List<String> desconocidos =
                iniciales.stream().filter(rol -> !CONOCIDOS.contains(rol)).sorted().toList();
        if (!desconocidos.isEmpty()) {
            throw new IllegalArgumentException(
                    "Roles desconocidos: " + String.join(", ", desconocidos));
        }
        roles.putIfAbsent(nombreUsuario, iniciales.stream().sorted().toList());
    }
}
