package ec.mdd.docentes.puertos.entrada;

import java.util.List;

/** Quiénes tienen proyectos asignables en un periodo, para la sugerencia (RN-13, ADR-023). */
public interface ConsultarDocentesConProyecto {

    /**
     * Los docentes que participan en algún proyecto asignable del periodo.
     *
     * @param periodo el código del periodo
     * @return por identificación
     */
    List<DocenteConProyecto> delPeriodo(String periodo);
}
