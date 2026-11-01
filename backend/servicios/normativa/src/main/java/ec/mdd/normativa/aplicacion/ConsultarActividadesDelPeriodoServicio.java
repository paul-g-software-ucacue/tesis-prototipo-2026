package ec.mdd.normativa.aplicacion;

import ec.mdd.normativa.puertos.entrada.ActividadesDeLaVersion;
import ec.mdd.normativa.puertos.entrada.ConsultarActividadesDelPeriodo;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.salida.RepositorioActividades;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;

/** El catálogo de actividades de la versión que rige un periodo (RF-21, D-79). */
public final class ConsultarActividadesDelPeriodoServicio
        implements ConsultarActividadesDelPeriodo {

    private final RepositorioVersiones versiones;
    private final RepositorioActividades actividades;

    /** Recibe los repositorios. */
    public ConsultarActividadesDelPeriodoServicio(
            RepositorioVersiones versiones, RepositorioActividades actividades) {
        this.versiones = versiones;
        this.actividades = actividades;
    }

    @Override
    public ActividadesDeLaVersion actividades(String periodo) {
        int version =
                versiones
                        .porPeriodo(periodo)
                        .orElseThrow(() -> new PeriodoSinVersion(periodo))
                        .numero();
        return new ActividadesDeLaVersion(version, actividades.deLaVersion(version));
    }
}
