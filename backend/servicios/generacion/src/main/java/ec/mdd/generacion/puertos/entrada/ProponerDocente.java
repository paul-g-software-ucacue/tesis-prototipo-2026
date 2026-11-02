package ec.mdd.generacion.puertos.entrada;

import java.util.Optional;

/** Proponer un docente para una asignatura-paralelo (RF-24, RN-19, D-19). */
public interface ProponerDocente {

    /**
     * Guarda la propuesta, o reemplaza la que tenga. La justificación la decide el dominio.
     *
     * @param solicitud lo que pide quien propone
     * @return la propuesta guardada, o la advertencia si falta la justificación de RN-19
     * @throws ParaleloNoEncontrado si no está en la oferta
     * @throws OtraCarrera si es de otra carrera
     * @throws ParaleloRetirado si se retiró
     * @throws PeriodoCerrado si el periodo se cerró
     * @throws SinDistributivo si el periodo no tiene distributivo
     * @throws PeriodoSinAbrir si el periodo no tiene su versión de normativa
     * @throws DocenteNoActivo si el docente no está activo
     * @throws HorasExcedidas si el docente queda sobre su límite
     * @throws TopeDeClase si queda sobre su tope de clase
     * @throws AfinidadInsuficiente si está bajo el mínimo y RN-19 es dura
     * @throws ec.mdd.generacion.dominio.JustificacionObligatoria si el texto tiene un largo
     *     inválido
     * @throws ec.mdd.generacion.dominio.ParcialNoEditable si el parcial ya no admite cambios
     */
    ResultadoDePropuesta proponer(SolicitudDePropuesta solicitud);

    /**
     * Quita la propuesta de una asignatura-paralelo, también si se retiró.
     *
     * @param carrera la única carrera que puede tocar; vacío, cualquiera (D-64)
     * @param quien el usuario que la quita
     * @throws ParaleloNoEncontrado si no está en la oferta
     * @throws OtraCarrera si es de otra carrera
     * @throws PeriodoCerrado si el periodo se cerró
     * @throws PropuestaNoEncontrada si no tiene propuesta
     * @throws ec.mdd.generacion.dominio.ParcialNoEditable si el parcial ya no admite cambios
     */
    void quitar(
            String periodo,
            String asignatura,
            String paralelo,
            Optional<String> carrera,
            String quien);
}
