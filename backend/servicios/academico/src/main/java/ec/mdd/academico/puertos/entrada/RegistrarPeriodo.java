package ec.mdd.academico.puertos.entrada;

import java.time.LocalDate;

/**
 * Caso de uso: registrar un periodo académico, en Planificación y sin versión de normativa (FR-001,
 * F02-H01). Refleja la operación {@code registrarPeriodo} del contrato de Académico. Es parte de la
 * API publicada del servicio: la carga 3 (F01) lo usará. Queda en el registro como {@code
 * PERIODO_REGISTRADO}.
 */
public interface RegistrarPeriodo {

    /**
     * @param quien el usuario de la sesión
     * @return el periodo registrado
     * @throws CodigoRepetido si ya hay un periodo con ese código
     * @throws ec.mdd.academico.dominio.DatoInvalido si un dato no cumple su regla
     */
    PeriodoConsultado registrar(
            String codigo, String nombre, LocalDate inicio, LocalDate fin, String quien);
}
