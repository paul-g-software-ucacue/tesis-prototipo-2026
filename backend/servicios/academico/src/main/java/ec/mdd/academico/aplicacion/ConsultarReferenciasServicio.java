package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.puertos.entrada.AsignaturaDeReferencia;
import ec.mdd.academico.puertos.entrada.CampoAmplioDeReferencia;
import ec.mdd.academico.puertos.entrada.CarreraDeReferencia;
import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.academico.puertos.entrada.PeriodoDeReferencia;
import ec.mdd.academico.puertos.entrada.ReferenciasDeAcademico;
import ec.mdd.academico.puertos.entrada.UnidadDeReferencia;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.util.Comparator;

/**
 * Las referencias para Docentes, sacadas de las carreras, las asignaturas y los campos amplios
 * (F03-H04), y de las unidades y los periodos (F03-H05), que ya guarda Académico.
 */
public final class ConsultarReferenciasServicio implements ConsultarReferencias {

    private final RepositorioCarreras repositorio;
    private final RepositorioEstructura estructura;
    private final RepositorioPeriodos periodos;
    private final NormativaDelPeriodo normativa;

    /**
     * Recibe los repositorios de las carreras, de la estructura orgánica y de los periodos, y la
     * versión de normativa de cada periodo (F02-H02).
     */
    public ConsultarReferenciasServicio(
            RepositorioCarreras repositorio,
            RepositorioEstructura estructura,
            RepositorioPeriodos periodos,
            NormativaDelPeriodo normativa) {
        this.repositorio = repositorio;
        this.estructura = estructura;
        this.periodos = periodos;
        this.normativa = normativa;
    }

    @Override
    public ReferenciasDeAcademico referencias() {
        return new ReferenciasDeAcademico(
                repositorio.carreras().stream()
                        .map(c -> new CarreraDeReferencia(c.codigo(), c.nombre(), c.siglas()))
                        .sorted(Comparator.comparing(CarreraDeReferencia::codigo))
                        .toList(),
                repositorio.asignaturas().stream()
                        .map(a -> new AsignaturaDeReferencia(a.codigo(), a.nombre(), a.carrera()))
                        .sorted(Comparator.comparing(AsignaturaDeReferencia::codigo))
                        .toList(),
                repositorio.camposAmplios().stream()
                        .map(c -> new CampoAmplioDeReferencia(c.codigo(), c.nombre()))
                        .sorted(Comparator.comparing(CampoAmplioDeReferencia::codigo))
                        .toList(),
                estructura.todas().stream()
                        .map(
                                u ->
                                        new UnidadDeReferencia(
                                                u.codigo(),
                                                u.nombre(),
                                                u.tipo().name(),
                                                u.gestionAutorizada(),
                                                u.activa()))
                        .sorted(Comparator.comparing(UnidadDeReferencia::codigo))
                        .toList(),
                periodos.todos().stream()
                        .map(
                                p ->
                                        new PeriodoDeReferencia(
                                                p.codigo(),
                                                p.nombre(),
                                                p.inicio(),
                                                p.fin(),
                                                normativa.versionDe(p.codigo()).isPresent(),
                                                p.estado() == EstadoPeriodo.CERRADO))
                        .sorted(Comparator.comparing(PeriodoDeReferencia::codigo))
                        .toList());
    }
}
