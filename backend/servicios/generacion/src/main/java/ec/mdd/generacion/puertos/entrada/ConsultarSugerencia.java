package ec.mdd.generacion.puertos.entrada;

import java.util.Optional;

/** Los docentes sugeridos para una asignatura-paralelo (RF-25, RN-19, ADR-010). */
public interface ConsultarSugerencia {

    /**
     * La sugerencia de una asignatura-paralelo ofertada.
     *
     * @param periodo el código del periodo
     * @param asignatura el código de la asignatura
     * @param paralelo el paralelo
     * @param carrera la única carrera que puede pedir; vacío, cualquiera (D-64)
     * @return los candidatos, en orden, y cuántos quedaron fuera por el mínimo
     * @throws ParaleloNoEncontrado si no está en la oferta del periodo
     * @throws OtraCarrera si es de otra carrera
     * @throws ParaleloRetirado si se retiró de la oferta
     * @throws PeriodoSinAbrir si el periodo no tiene su versión de normativa
     */
    SugerenciaDelParalelo sugerencia(
            String periodo, String asignatura, String paralelo, Optional<String> carrera);
}
