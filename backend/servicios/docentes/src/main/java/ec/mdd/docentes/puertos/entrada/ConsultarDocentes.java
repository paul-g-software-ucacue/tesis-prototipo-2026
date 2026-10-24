package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.BusquedaDeDocentes;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.Referencias;
import java.util.List;

/**
 * Consultar los docentes con su ficha académica (F03-H04, RF-16, RF-17). Es parte de la API
 * publicada del servicio: Generación la usará para el puntaje de afinidad (F06-H01).
 */
public interface ConsultarDocentes {

    /** Los docentes que cumplen los criterios, por apellidos y nombres. */
    List<Docente> buscar(BusquedaDeDocentes busqueda);

    /**
     * @throws DocenteNoEncontrado si no hay un docente con esa identificación
     */
    Docente docente(String identificacion);

    /** Las carreras, las asignaturas y los campos amplios de Académico, con sus nombres. */
    Referencias referencias();
}
