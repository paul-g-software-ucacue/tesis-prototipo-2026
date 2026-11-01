package ec.mdd.generacion.puertos.entrada;

import java.util.List;

/** Las actividades del catálogo y si se le pueden asignar a un docente (RN-22, RN-25). */
public interface ConsultarActividadesPosibles {

    /**
     * Todas las actividades del catálogo, cada una con su disponibilidad.
     *
     * @param periodo el código del periodo
     * @param docente la identificación del docente
     * @return en el orden de la norma
     * @throws DocenteNoActivo si el docente no está activo
     * @throws PeriodoSinAbrir si el periodo no tiene su versión de normativa
     */
    List<ActividadPosible> posibles(String periodo, String docente);
}
