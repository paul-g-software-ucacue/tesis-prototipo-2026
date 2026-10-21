package ec.mdd.academico.puertos.entrada;

import ec.mdd.academico.dominio.EstadoPeriodo;

/**
 * Caso de uso: pasar un periodo al estado siguiente (RF-13, FR-004). Refleja la operación {@code
 * cambiarEstadoPeriodo} del contrato de Académico. Recibe el estado de destino, para que un pedido
 * repetido no avance dos pasos. Queda en el registro como {@code PERIODO_ESTADO_CAMBIADO}.
 */
public interface CambiarEstadoPeriodo {

    /**
     * @param destino el estado siguiente; si es el actual, no cambia nada
     * @param quien el usuario de la sesión
     * @return el periodo en su estado nuevo
     * @throws PeriodoNoEncontrado si no existe
     * @throws ec.mdd.academico.dominio.PeriodoCerrado si está cerrado
     * @throws ec.mdd.academico.dominio.TransicionNoPermitida si el destino no es el siguiente
     * @throws ec.mdd.academico.dominio.PeriodoSinVersion si sale de Planificación sin versión
     */
    PeriodoConsultado cambiar(String codigo, EstadoPeriodo destino, String quien);
}
