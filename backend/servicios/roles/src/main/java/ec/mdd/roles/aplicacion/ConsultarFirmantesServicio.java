package ec.mdd.roles.aplicacion;

import ec.mdd.roles.puertos.entrada.ConsultarFirmantes;
import ec.mdd.roles.puertos.entrada.Firmantes;
import ec.mdd.roles.puertos.salida.RepositorioAsignacionesDeRol;
import ec.mdd.roles.puertos.salida.RepositorioDesignaciones;
import java.util.List;
import java.util.Map;

/** Quién puede firmar en un periodo: los roles con usuario y si hay Responsable (RF-58). */
public final class ConsultarFirmantesServicio implements ConsultarFirmantes {

    private final RepositorioAsignacionesDeRol asignaciones;
    private final RepositorioDesignaciones designaciones;

    /** Recibe los repositorios. */
    public ConsultarFirmantesServicio(
            RepositorioAsignacionesDeRol asignaciones, RepositorioDesignaciones designaciones) {
        this.asignaciones = asignaciones;
        this.designaciones = designaciones;
    }

    @Override
    public Firmantes firmantes(String periodo) {
        List<String> conUsuario =
                asignaciones.usuariosPorRol().entrySet().stream()
                        .filter(rol -> rol.getValue() > 0)
                        .map(Map.Entry::getKey)
                        .sorted()
                        .toList();
        return new Firmantes(conUsuario, designaciones.dePeriodo(periodo).isPresent());
    }
}
