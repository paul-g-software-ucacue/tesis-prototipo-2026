package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.AfinidadDelDocente;

/** La afinidad de un docente en la oferta de un periodo (RF-17, RN-24, ADR-010). */
public interface ConsultarAfinidad {

    /**
     * El puntaje del docente en cada asignatura de la oferta del periodo.
     *
     * @param identificacion el número de identificación del docente
     * @param periodo el código del periodo
     * @return los puntajes con su desglose y la versión que dio los pesos
     * @throws PeriodoNoEncontrado si el periodo no existe
     * @throws PeriodoSinAbrir si el periodo no tiene su versión de normativa
     * @throws DocenteNoEncontrado si el docente no existe
     */
    AfinidadDelDocente afinidad(String identificacion, String periodo);
}
