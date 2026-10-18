package ec.mdd.usuarios;

import ec.mdd.usuarios.puertos.salida.AutorizacionDeUsuario;
import ec.mdd.usuarios.puertos.salida.ProveedorDeAutorizacion;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Doble de {@link ProveedorDeAutorizacion} escrito a mano: anota los roles iniciales pedidos. Roles
 * tiene sus propias pruebas con PostgreSQL; aquí solo interesa qué pide Usuarios.
 */
final class AutorizacionAnotada implements ProveedorDeAutorizacion {

    final Map<String, List<String>> pedidos = new LinkedHashMap<>();

    @Override
    public AutorizacionDeUsuario consultar(String nombreUsuario) {
        return AutorizacionDeUsuario.NINGUNA;
    }

    @Override
    public void asignarRolesIniciales(String nombreUsuario, Collection<String> roles) {
        pedidos.put(nombreUsuario, roles.stream().sorted().toList());
    }
}
