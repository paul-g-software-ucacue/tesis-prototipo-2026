package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.AfinidadDelDocente;
import java.util.Optional;

/** La afinidad de un docente en la oferta de un periodo (RF-17, RN-24, ADR-010). */
public interface ConsultarAfinidad {

    /**
     * El puntaje del docente en cada asignatura de la oferta del periodo.
     *
     * @param identificacion el número de identificación del docente
     * @param periodo el código del periodo
     * @param carrera solo las asignaturas de esta carrera; vacío, todas (D-64)
     * @return los puntajes con su desglose y la versión que dio los pesos
     * @throws PeriodoNoEncontrado si el periodo no existe
     * @throws PeriodoSinAbrir si el periodo no tiene su versión de normativa
     * @throws DocenteNoEncontrado si el docente no existe
     */
    AfinidadDelDocente afinidad(String identificacion, String periodo, Optional<String> carrera);
}
