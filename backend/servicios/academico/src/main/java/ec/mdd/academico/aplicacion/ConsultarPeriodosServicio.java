package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.entrada.ConsultarPeriodos;
import ec.mdd.academico.puertos.entrada.PeriodoConsultado;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.util.List;

/**
 * Consultar los periodos con su versión de normativa, que se pide a Normativa por cada periodo: es
 * la única que la guarda (research §2).
 */
public final class ConsultarPeriodosServicio implements ConsultarPeriodos {

    private final RepositorioPeriodos repositorio;
    private final NormativaDelPeriodo normativa;

    /** Recibe el repositorio y la normativa de los periodos. */
    public ConsultarPeriodosServicio(
            RepositorioPeriodos repositorio, NormativaDelPeriodo normativa) {
        this.repositorio = repositorio;
        this.normativa = normativa;
    }

    @Override
    public List<PeriodoConsultado> todos() {
        return repositorio.todos().stream().map(this::conVersion).toList();
    }

    @Override
    public PeriodoConsultado porCodigo(String codigo) {
        return conVersion(
                repositorio.porCodigo(codigo).orElseThrow(() -> new PeriodoNoEncontrado(codigo)));
    }

    private PeriodoConsultado conVersion(PeriodoAcademico periodo) {
        return new PeriodoConsultado(periodo, normativa.versionDe(periodo.codigo()));
    }
}
