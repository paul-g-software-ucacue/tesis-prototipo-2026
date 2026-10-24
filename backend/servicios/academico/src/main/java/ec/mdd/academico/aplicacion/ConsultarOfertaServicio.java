package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.CampoAmplio;
import ec.mdd.academico.dominio.DatosMaestros;
import ec.mdd.academico.dominio.OfertaDelPeriodo;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.entrada.ConsultarOferta;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.academico.puertos.salida.RepositorioOferta;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;

/**
 * Consultar la oferta de un periodo. Las horas a cubrir, las horas y la sede de cada
 * asignatura-paralelo se calculan al consultar, con los datos maestros de ese momento (FR-007).
 */
public final class ConsultarOfertaServicio implements ConsultarOferta {

    private final RepositorioPeriodos periodos;
    private final RepositorioOferta repositorio;
    private final RepositorioCarreras carreras;
    private final RepositorioEstructura estructura;

    /** Recibe los repositorios de los periodos, la oferta, las carreras y la estructura. */
    public ConsultarOfertaServicio(
            RepositorioPeriodos periodos,
            RepositorioOferta repositorio,
            RepositorioCarreras carreras,
            RepositorioEstructura estructura) {
        this.periodos = periodos;
        this.repositorio = repositorio;
        this.carreras = carreras;
        this.estructura = estructura;
    }

    @Override
    public OfertaDelPeriodo oferta(String codigo) {
        PeriodoAcademico periodo =
                periodos.porCodigo(codigo).orElseThrow(() -> new PeriodoNoEncontrado(codigo));
        return new OfertaDelPeriodo(
                periodo, repositorio.delPeriodo(codigo), datosMaestros(carreras, estructura));
    }

    /** Las carreras, mallas, asignaturas, la estructura y los campos amplios, juntos. */
    static DatosMaestros datosMaestros(
            RepositorioCarreras carreras, RepositorioEstructura estructura) {
        return new DatosMaestros(
                carreras.carreras(),
                carreras.mallas(),
                carreras.asignaturas(),
                estructura.todas(),
                carreras.camposAmplios().stream().map(CampoAmplio::codigo).toList());
    }
}
