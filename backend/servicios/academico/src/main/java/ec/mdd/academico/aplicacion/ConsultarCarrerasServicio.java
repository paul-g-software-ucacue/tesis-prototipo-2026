package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoAmplio;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.puertos.entrada.AsignaturaNoEncontrada;
import ec.mdd.academico.puertos.entrada.CarreraConMallas;
import ec.mdd.academico.puertos.entrada.CarreraNoEncontrada;
import ec.mdd.academico.puertos.entrada.ConsultarCarreras;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import java.math.BigDecimal;
import java.util.List;

/**
 * Consultar las carreras, sus mallas y sus asignaturas. Las horas de cada malla se calculan al
 * consultar, con sus asignaturas activas (FR-012): no se guardan.
 */
public final class ConsultarCarrerasServicio implements ConsultarCarreras {

    private final RepositorioCarreras repositorio;

    /** Recibe el repositorio de las carreras. */
    public ConsultarCarrerasServicio(RepositorioCarreras repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<CampoAmplio> camposAmplios() {
        return repositorio.camposAmplios();
    }

    @Override
    public List<CarreraConMallas> carreras() {
        List<Malla> mallas = repositorio.mallas();
        List<Asignatura> asignaturas = repositorio.asignaturas();
        return repositorio.carreras().stream()
                .map(carrera -> conMallas(carrera, mallas, asignaturas))
                .toList();
    }

    @Override
    public CarreraConMallas carrera(String codigo) {
        return conMallas(existente(codigo), repositorio.mallas(), repositorio.asignaturas());
    }

    @Override
    public List<Asignatura> asignaturasDe(String carrera) {
        existente(carrera);
        return repositorio.asignaturas().stream()
                .filter(asignatura -> asignatura.carrera().equals(carrera))
                .toList();
    }

    @Override
    public Asignatura asignatura(String codigo) {
        return repositorio.asignaturas().stream()
                .filter(asignatura -> asignatura.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AsignaturaNoEncontrada(codigo));
    }

    private Carrera existente(String codigo) {
        return repositorio.carreras().stream()
                .filter(carrera -> carrera.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new CarreraNoEncontrada(codigo));
    }

    private static CarreraConMallas conMallas(
            Carrera carrera, List<Malla> mallas, List<Asignatura> asignaturas) {
        return new CarreraConMallas(
                carrera,
                mallas.stream()
                        .filter(malla -> malla.carrera().equals(carrera.codigo()))
                        .map(malla -> resumen(malla, asignaturas))
                        .toList());
    }

    private static CarreraConMallas.Resumen resumen(Malla malla, List<Asignatura> asignaturas) {
        List<Asignatura> activas =
                asignaturas.stream()
                        .filter(a -> a.activa() && a.malla().equals(malla.codigo()))
                        .toList();
        return new CarreraConMallas.Resumen(
                malla,
                activas.size(),
                activas.stream()
                        .map(Asignatura::horasSemanalesClase)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
