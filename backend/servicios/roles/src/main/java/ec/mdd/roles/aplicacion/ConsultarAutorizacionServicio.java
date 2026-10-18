package ec.mdd.roles.aplicacion;

import ec.mdd.roles.dominio.NombreDeUsuario;
import ec.mdd.roles.dominio.PermisosEfectivos;
import ec.mdd.roles.dominio.Rol;
import ec.mdd.roles.puertos.entrada.Autorizacion;
import ec.mdd.roles.puertos.entrada.ConsultarAutorizacion;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioRoles;
import java.util.List;

/** Une los permisos de los roles asignados al usuario (FR-005). */
public final class ConsultarAutorizacionServicio implements ConsultarAutorizacion {

    private final RepositorioRoles roles;
    private final RepositorioAsignacionesDeRol asignaciones;

    /** Recibe los puertos de salida. */
    public ConsultarAutorizacionServicio(
            RepositorioRoles roles, RepositorioAsignacionesDeRol asignaciones) {
        this.roles = roles;
        this.asignaciones = asignaciones;
    }

    @Override
    public Autorizacion consultar(String usuario) {
        String nombre = NombreDeUsuario.normalizar(usuario);
        List<String> suyos = asignaciones.rolesDe(nombre);
        if (suyos.isEmpty()) {
            return new Autorizacion(nombre, List.of(), List.of());
        }
        List<Rol> asignados =
                roles.todos().stream().filter(rol -> suyos.contains(rol.identificador())).toList();
        PermisosEfectivos efectivos = PermisosEfectivos.de(asignados);
        return new Autorizacion(nombre, efectivos.roles(), efectivos.textos());
    }
}
