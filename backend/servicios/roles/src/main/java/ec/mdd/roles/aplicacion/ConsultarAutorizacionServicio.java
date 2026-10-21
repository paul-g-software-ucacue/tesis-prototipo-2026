package ec.mdd.roles.aplicacion;

import ec.mdd.roles.dominio.Designacion;
import ec.mdd.roles.dominio.NombreDeUsuario;
import ec.mdd.roles.dominio.PermisosEfectivos;
import ec.mdd.roles.dominio.Rol;
import ec.mdd.roles.puertos.entrada.Autorizacion;
import ec.mdd.roles.puertos.entrada.ConsultarAutorizacion;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioDesignaciones;
import ec.mdd.roles.puertos.salida.RepositorioRoles;
import java.util.ArrayList;
import java.util.List;

/**
 * Une los permisos de los roles asignados al usuario (FR-005) y, si tiene una designación en un
 * periodo no cerrado, los del Responsable de consolidación (F05-H03, research §3). Una asignación
 * global del mismo rol, hecha a mano en «Roles», sigue valiendo aparte.
 */
public final class ConsultarAutorizacionServicio implements ConsultarAutorizacion {

    private final RepositorioRoles roles;
    private final RepositorioAsignacionesDeRol asignaciones;
    private final RepositorioDesignaciones designaciones;

    /** Recibe los puertos de salida. */
    public ConsultarAutorizacionServicio(
            RepositorioRoles roles,
            RepositorioAsignacionesDeRol asignaciones,
            RepositorioDesignaciones designaciones) {
        this.roles = roles;
        this.asignaciones = asignaciones;
        this.designaciones = designaciones;
    }

    @Override
    public Autorizacion consultar(String usuario) {
        String nombre = NombreDeUsuario.normalizar(usuario);
        List<String> suyos = new ArrayList<>(asignaciones.rolesDe(nombre));
        if (!designaciones.periodosAbiertosDe(nombre).isEmpty()
                && !suyos.contains(Designacion.ROL)) {
            suyos.add(Designacion.ROL);
        }
        if (suyos.isEmpty()) {
            return new Autorizacion(nombre, List.of(), List.of());
        }
        List<Rol> asignados =
                roles.todos().stream().filter(rol -> suyos.contains(rol.identificador())).toList();
        PermisosEfectivos efectivos = PermisosEfectivos.de(asignados);
        return new Autorizacion(nombre, efectivos.roles(), efectivos.textos());
    }
}
