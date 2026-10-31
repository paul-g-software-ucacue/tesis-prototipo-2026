package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.Sugerencia;

/** Los docentes sugeridos para una asignatura-paralelo (RF-25, RN-19, ADR-010). */
public interface ConsultarSugerencia {

    /**
     * La sugerencia de una asignatura-paralelo ofertada.
     *
     * @param periodo el código del periodo
     * @param asignatura el código de la asignatura
     * @param paralelo el paralelo
     * @return los candidatos, en orden, y cuántos quedaron fuera por el mínimo
     * @throws ParaleloNoEncontrado si no está en la oferta del periodo
     * @throws ParaleloRetirado si se retiró de la oferta
     * @throws PeriodoSinAbrir si el periodo no tiene su versión de normativa
     */
    Sugerencia sugerencia(String periodo, String asignatura, String paralelo);
}
