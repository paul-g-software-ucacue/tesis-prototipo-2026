package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.BusquedaDeDocentes;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.Referencias;
import ec.mdd.docentes.puertos.entrada.ConsultarDocentes;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import java.util.List;

/**
 * Consultar los docentes. La búsqueda filtra en memoria: un IST tiene decenas o pocos cientos de
 * docentes, y así no hacen falta extensiones de la base ni columnas normalizadas (research §6).
 */
public final class ConsultarDocentesServicio implements ConsultarDocentes {

    private final RepositorioDocentes repositorio;
    private final ReferenciasAcademicas referencias;

    /** Recibe el repositorio de los docentes y las referencias de Académico. */
    public ConsultarDocentesServicio(
            RepositorioDocentes repositorio, ReferenciasAcademicas referencias) {
        this.repositorio = repositorio;
        this.referencias = referencias;
    }

    @Override
    public List<Docente> buscar(BusquedaDeDocentes busqueda) {
        return repositorio.todos().stream().filter(busqueda::aplicaA).toList();
    }

    @Override
    public Docente docente(String identificacion) {
        return repositorio.buscar(identificacion).orElseThrow(DocenteNoEncontrado::new);
    }

    @Override
    public Referencias referencias() {
        return referencias.referencias();
    }
}
