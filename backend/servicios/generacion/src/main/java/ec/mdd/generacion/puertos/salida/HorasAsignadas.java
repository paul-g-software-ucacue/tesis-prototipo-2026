package ec.mdd.generacion.puertos.salida;

import ec.mdd.generacion.dominio.HorasDelDocente;
import java.util.Map;

/** Las horas ya asignadas a los docentes en un periodo, en todas las carreras y unidades (D-01). */
public interface HorasAsignadas {

    /**
     * Las horas de los docentes que tienen asignaciones.
     *
     * @param periodo el código del periodo
     * @param carrera la carrera desde la que se mira: sus horas no cuentan como «otras carreras»
     * @return por número de identificación; un docente que no está no tiene horas asignadas
     */
    Map<String, HorasDelDocente> delPeriodo(String periodo, String carrera);
}
