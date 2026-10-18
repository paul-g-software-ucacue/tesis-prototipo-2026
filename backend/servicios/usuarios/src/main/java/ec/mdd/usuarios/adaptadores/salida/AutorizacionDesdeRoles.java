package ec.mdd.usuarios.adaptadores.salida;

import ec.mdd.roles.puertos.entrada.AsignarRolesIniciales;
import ec.mdd.roles.puertos.entrada.Autorizacion;
import ec.mdd.roles.puertos.entrada.ConsultarAutorizacion;
import ec.mdd.usuarios.puertos.salida.AutorizacionDeUsuario;
import ec.mdd.usuarios.puertos.salida.ProveedorDeAutorizacion;
import java.util.Collection;

/**
 * {@link ProveedorDeAutorizacion} sobre la interfaz publicada de Roles en el mismo proceso
 * (principio V, research §2). Usuarios solo ve {@code ec.mdd.roles.puertos.entrada}; Roles no
 * conoce a Usuarios.
 */
public final class AutorizacionDesdeRoles implements ProveedorDeAutorizacion {

    private final ConsultarAutorizacion consultar;
    private final AsignarRolesIniciales iniciales;

    /** Recibe los casos de uso publicados por Roles. */
    public AutorizacionDesdeRoles(
            ConsultarAutorizacion consultar, AsignarRolesIniciales iniciales) {
        this.consultar = consultar;
        this.iniciales = iniciales;
    }

    @Override
    public AutorizacionDeUsuario consultar(String nombreUsuario) {
        Autorizacion autorizacion = consultar.consultar(nombreUsuario);
        return new AutorizacionDeUsuario(autorizacion.roles(), autorizacion.permisos());
    }

    @Override
    public void asignarRolesIniciales(String nombreUsuario, Collection<String> roles) {
        iniciales.asignar(nombreUsuario, roles);
    }
}
