package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.Referencias;
import ec.mdd.docentes.puertos.entrada.AsignablesDelPeriodo;
import ec.mdd.docentes.puertos.entrada.ConsultarAsignablesDelPeriodo;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RepositorioProyectos;

/**
 * Si un periodo tiene proyectos asignables (F01-H05), con la misma regla que la consulta de los
 * proyectos (F03-H05): aprobado o en ejecución y una vigencia que se cruza con el periodo.
 */
public final class ConsultarAsignablesServicio implements ConsultarAsignablesDelPeriodo {

    private final RepositorioProyectos repositorio;
    private final ReferenciasAcademicas referencias;

    /** Recibe los proyectos y las referencias de Académico. */
    public ConsultarAsignablesServicio(
            RepositorioProyectos repositorio, ReferenciasAcademicas referencias) {
        this.repositorio = repositorio;
        this.referencias = referencias;
    }

    @Override
    public AsignablesDelPeriodo asignablesEn(String periodo) {
        Referencias.Periodo elPeriodo;
        try {
            elPeriodo = referencias.referencias().periodo(periodo);
        } catch (DatoInvalido noExiste) {
            return AsignablesDelPeriodo.PERIODO_NO_ENCONTRADO;
        }
        boolean alguno =
                repositorio.todos().stream()
                        .anyMatch(
                                proyecto ->
                                        proyecto.asignableEn(elPeriodo.inicio(), elPeriodo.fin())
                                                .asignable());
        return alguno ? AsignablesDelPeriodo.ALGUNO : AsignablesDelPeriodo.NINGUNO;
    }
}
