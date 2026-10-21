package ec.mdd.academico.puertos.entrada;

import java.util.List;

/**
 * Caso de uso: consultar los periodos académicos con su estado y su versión de normativa (FR-009).
 * Refleja las operaciones {@code listarPeriodos} y {@code consultarPeriodo} del contrato de
 * Académico.
 */
public interface ConsultarPeriodos {

    /** Todos los periodos, del de inicio más reciente al más antiguo. */
    List<PeriodoConsultado> todos();

    /**
     * @throws PeriodoNoEncontrado si no hay un periodo con ese código
     */
    PeriodoConsultado porCodigo(String codigo);
}
