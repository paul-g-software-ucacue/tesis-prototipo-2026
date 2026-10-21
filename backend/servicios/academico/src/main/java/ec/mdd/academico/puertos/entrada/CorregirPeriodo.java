package ec.mdd.academico.puertos.entrada;

import java.time.LocalDate;

/**
 * Caso de uso: corregir el nombre y las fechas de un periodo que no está cerrado (F02-H01). Refleja
 * la operación {@code corregirPeriodo} del contrato de Académico. Si algo cambia, queda en el
 * registro como {@code PERIODO_CORREGIDO}, con cada valor anterior y nuevo.
 */
public interface CorregirPeriodo {

    /**
     * @param quien el usuario de la sesión
     * @return el periodo corregido
     * @throws PeriodoNoEncontrado si no existe
     * @throws ec.mdd.academico.dominio.PeriodoCerrado si está cerrado
     * @throws ec.mdd.academico.dominio.DatoInvalido si un dato no cumple su regla
     */
    PeriodoConsultado corregir(
            String codigo, String nombre, LocalDate inicio, LocalDate fin, String quien);
}
