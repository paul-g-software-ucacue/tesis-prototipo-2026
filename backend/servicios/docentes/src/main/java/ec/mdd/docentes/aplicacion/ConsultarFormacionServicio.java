package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.FichaAcademica;
import ec.mdd.docentes.puertos.entrada.ConsultarFormacion;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;

/** La ficha académica de un docente, en cualquier estado laboral. */
public final class ConsultarFormacionServicio implements ConsultarFormacion {

    private final RepositorioDocentes repositorio;

    /** Recibe el repositorio de los docentes. */
    public ConsultarFormacionServicio(RepositorioDocentes repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public FichaAcademica formacion(String identificacion) {
        return repositorio
                .buscar(identificacion)
                .orElseThrow(DocenteNoEncontrado::new)
                .datos()
                .ficha();
    }
}
