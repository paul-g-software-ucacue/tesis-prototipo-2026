package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.EstadoLaboral;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentesActivos;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import java.util.List;

/** Los docentes activos: solo un docente activo recibe asignaciones. */
public final class ConsultarDocentesActivosServicio implements ConsultarDocentesActivos {

    private final RepositorioDocentes repositorio;

    /** Recibe el repositorio de los docentes. */
    public ConsultarDocentesActivosServicio(RepositorioDocentes repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<Docente> activos() {
        return repositorio.todos().stream()
                .filter(docente -> docente.estado() == EstadoLaboral.ACTIVO)
                .toList();
    }
}
