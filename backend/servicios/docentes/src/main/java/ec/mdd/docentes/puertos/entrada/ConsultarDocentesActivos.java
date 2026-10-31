package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.Docente;
import java.util.List;

/** Los docentes activos, para sugerirlos en una asignatura-paralelo (F06-H02, ADR-023). */
public interface ConsultarDocentesActivos {

    /**
     * Los docentes activos, de todas las carreras base.
     *
     * @return por apellidos y nombres
     */
    List<Docente> activos();
}
