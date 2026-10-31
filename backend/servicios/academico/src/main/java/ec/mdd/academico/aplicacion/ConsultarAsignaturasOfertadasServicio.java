package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.AsignaturaParalelo;
import ec.mdd.academico.dominio.EstadoDeAsignaturaParalelo;
import ec.mdd.academico.puertos.entrada.AsignaturaConCampos;
import ec.mdd.academico.puertos.entrada.ConsultarAsignaturasOfertadas;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioOferta;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Las asignaturas ofertadas de un periodo, en cualquier estado del periodo: uno cerrado conserva su
 * oferta. Las retiradas no cuentan (RF-15).
 */
public final class ConsultarAsignaturasOfertadasServicio implements ConsultarAsignaturasOfertadas {

    private final RepositorioPeriodos periodos;
    private final RepositorioOferta oferta;
    private final RepositorioCarreras carreras;

    /** Recibe los repositorios de los periodos, la oferta y las carreras. */
    public ConsultarAsignaturasOfertadasServicio(
            RepositorioPeriodos periodos, RepositorioOferta oferta, RepositorioCarreras carreras) {
        this.periodos = periodos;
        this.oferta = oferta;
        this.carreras = carreras;
    }

    @Override
    public List<AsignaturaConCampos> delPeriodo(String periodo, Optional<String> carrera) {
        periodos.porCodigo(periodo).orElseThrow(() -> new PeriodoNoEncontrado(periodo));
        Set<String> ofertadas =
                oferta.delPeriodo(periodo).stream()
                        .filter(
                                paralelo ->
                                        paralelo.estado() == EstadoDeAsignaturaParalelo.OFERTADA)
                        .map(AsignaturaParalelo::asignatura)
                        .collect(Collectors.toSet());
        return carreras.asignaturas().stream()
                .filter(asignatura -> ofertadas.contains(asignatura.codigo()))
                .filter(
                        asignatura ->
                                carrera.isEmpty() || carrera.get().equals(asignatura.carrera()))
                .map(
                        asignatura ->
                                new AsignaturaConCampos(
                                        asignatura.codigo(),
                                        asignatura.nombre(),
                                        asignatura.campo().amplio(),
                                        asignatura.campo().especifico()))
                .distinct()
                .toList();
    }
}
