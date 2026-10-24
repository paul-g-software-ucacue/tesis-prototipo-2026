package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoAmplio;
import java.util.List;

/**
 * Caso de uso: consultar las carreras, sus mallas y sus asignaturas, y los campos amplios de la
 * CINE (FR-007, FR-010, FR-012, F03-H02). Es parte de la API publicada del servicio: la oferta
 * (F03-H03), las cargas (F01-H04) y la afinidad (F07) la usarán.
 */
public interface ConsultarCarreras {

    /** Los 11 campos amplios de la CINE, por código. */
    List<CampoAmplio> camposAmplios();

    /** Las carreras por código, cada una con el resumen de sus mallas. */
    List<CarreraConMallas> carreras();

    /**
     * @throws CarreraNoEncontrada si no hay una carrera con ese código
     */
    CarreraConMallas carrera(String codigo);

    /**
     * Las asignaturas de la carrera, de todas sus mallas, por código.
     *
     * @throws CarreraNoEncontrada si no hay una carrera con ese código
     */
    List<Asignatura> asignaturasDe(String carrera);

    /**
     * @throws AsignaturaNoEncontrada si no hay una asignatura con ese código
     */
    Asignatura asignatura(String codigo);
}
