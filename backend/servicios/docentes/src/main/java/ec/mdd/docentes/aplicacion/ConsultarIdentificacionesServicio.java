package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.puertos.entrada.ConsultarIdentificaciones;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import java.util.Set;
import java.util.stream.Collectors;

/** Las identificaciones de los docentes registrados, para Cargas (F01-H03). */
public final class ConsultarIdentificacionesServicio implements ConsultarIdentificaciones {

    private final RepositorioDocentes repositorio;

    /** Recibe el repositorio de los docentes. */
    public ConsultarIdentificacionesServicio(RepositorioDocentes repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Set<String> identificaciones() {
        return repositorio.todos().stream()
                .map(docente -> docente.identificacion().numero())
                .collect(Collectors.toUnmodifiableSet());
    }
}
