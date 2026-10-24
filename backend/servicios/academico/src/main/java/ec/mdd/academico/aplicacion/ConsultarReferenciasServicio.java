package ec.mdd.academico.aplicacion;

import ec.mdd.academico.puertos.entrada.AsignaturaDeReferencia;
import ec.mdd.academico.puertos.entrada.CampoAmplioDeReferencia;
import ec.mdd.academico.puertos.entrada.CarreraDeReferencia;
import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.academico.puertos.entrada.ReferenciasDeAcademico;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import java.util.Comparator;

/**
 * Las referencias para Docentes (F03-H04), sacadas de las carreras, las asignaturas y los campos
 * amplios que ya guarda Académico.
 */
public final class ConsultarReferenciasServicio implements ConsultarReferencias {

    private final RepositorioCarreras repositorio;

    /** Recibe el repositorio de las carreras. */
    public ConsultarReferenciasServicio(RepositorioCarreras repositorio) {
        this.repositorio = repositorio;
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
                        .toList());
    }
}
