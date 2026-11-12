package ec.mdd.roles.aplicacion;

import ec.mdd.roles.dominio.Designacion;
import ec.mdd.roles.puertos.entrada.ConsultarResponsable;
import ec.mdd.roles.puertos.entrada.ResponsableDelPeriodo;
import ec.mdd.roles.puertos.entrada.SinResponsable;
import ec.mdd.roles.puertos.requeridos.NombreDeUsuario;
import ec.mdd.roles.puertos.salida.RepositorioDesignaciones;

/**
 * El Responsable de consolidación designado en el periodo y su nombre para mostrar (ADR-003,
 * F13-H02). El nombre lo da Usuarios por el puerto que Roles requiere; si no tiene, va el nombre de
 * usuario. Solo lee.
 */
public final class ConsultarResponsableServicio implements ConsultarResponsable {

    private final RepositorioDesignaciones designaciones;
    private final NombreDeUsuario nombres;

    /** Recibe las designaciones y el nombre de Usuarios. */
    public ConsultarResponsableServicio(
            RepositorioDesignaciones designaciones, NombreDeUsuario nombres) {
        this.designaciones = designaciones;
        this.nombres = nombres;
    }

    @Override
    public ResponsableDelPeriodo responsable(String periodo) {
        Designacion designacion = designaciones.dePeriodo(periodo).orElseThrow(SinResponsable::new);
        String usuario = designacion.usuario();
        return new ResponsableDelPeriodo(
                usuario, nombres.nombre(usuario).filter(n -> !n.isBlank()).orElse(usuario));
    }
}
