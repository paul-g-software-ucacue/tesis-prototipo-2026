package ec.mdd.cargas.adaptadores.salida;

import ec.mdd.academico.puertos.entrada.AsignaturaDeReferencia;
import ec.mdd.academico.puertos.entrada.CarreraDeReferencia;
import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.academico.puertos.entrada.PeriodoDeReferencia;
import ec.mdd.academico.puertos.entrada.ReferenciasDeAcademico;
import ec.mdd.academico.puertos.entrada.UnidadDeReferencia;
import ec.mdd.cargas.dominio.ReferenciasExistentes;
import ec.mdd.cargas.puertos.salida.PeriodoAcademico;
import ec.mdd.cargas.puertos.salida.ReferenciasDelSistema;
import ec.mdd.docentes.puertos.entrada.ConsultarIdentificaciones;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Las referencias del sistema, pedidas por las interfaces publicadas de Académico y de Docentes
 * dentro del proceso {@code datos}: solo sus {@code puertos.entrada}, como Docentes con Académico;
 * {@code ModulosTest} lo comprueba (principio V, research §6). Cuentan todas, activas o no: si
 * están vigentes lo decide el dueño al confirmar la carga (F01-H04).
 */
public final class ReferenciasDesdePuertos implements ReferenciasDelSistema {

    private final ConsultarReferencias academico;
    private final ConsultarIdentificaciones docentes;

    /** Recibe los puertos publicados de Académico y de Docentes. */
    public ReferenciasDesdePuertos(
            ConsultarReferencias academico, ConsultarIdentificaciones docentes) {
        this.academico = academico;
        this.docentes = docentes;
    }

    @Override
    public ReferenciasExistentes existentes() {
        ReferenciasDeAcademico referencias = academico.referencias();
        return new ReferenciasExistentes(
                codigos(referencias.unidades(), UnidadDeReferencia::codigo),
                codigos(referencias.carreras(), CarreraDeReferencia::codigo),
                codigos(referencias.periodos(), PeriodoDeReferencia::codigo),
                codigos(referencias.asignaturas(), AsignaturaDeReferencia::codigo),
                docentes.identificaciones());
    }

    @Override
    public List<PeriodoAcademico> periodos() {
        return academico.referencias().periodos().stream()
                .sorted(Comparator.comparing(PeriodoDeReferencia::inicio).reversed())
                .map(
                        periodo ->
                                new PeriodoAcademico(
                                        periodo.codigo(),
                                        periodo.nombre(),
                                        periodo.inicio(),
                                        periodo.abierto(),
                                        periodo.cerrado()))
                .toList();
    }

    private static <T> Set<String> codigos(List<T> lista, Function<T, String> codigo) {
        return lista.stream().map(codigo).collect(Collectors.toUnmodifiableSet());
    }
}
