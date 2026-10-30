package ec.mdd.cargas.puertos.entrada;

import ec.mdd.cargas.dominio.ConfirmacionSinProyectos;

/**
 * Caso de uso: confirmar que un periodo no tiene proyectos aprobados (RF-18, F01-H05, glosario
 * «Confirmación "sin proyectos"»). Refleja {@code confirmarSinProyectos}.
 */
public interface ConfirmarSinProyectos {

    /**
     * Guarda la confirmación en el historial de la carga de proyectos, o devuelve la que ya estaba.
     *
     * @param periodo el código de un periodo de los ciclos académicos
     * @param quien el usuario que confirma
     * @throws ec.mdd.cargas.puertos.salida.PeriodoNoEncontrado si el periodo no está en los ciclos
     * @throws PeriodoConProyectos si el periodo tiene proyectos asignables
     */
    ConfirmacionSinProyectos confirmar(String periodo, String quien);
}
