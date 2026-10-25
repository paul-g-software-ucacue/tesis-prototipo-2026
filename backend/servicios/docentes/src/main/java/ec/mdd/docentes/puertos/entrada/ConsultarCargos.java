package ec.mdd.docentes.puertos.entrada;

import ec.mdd.docentes.dominio.EstadoDeCargo;
import java.util.List;

/**
 * Caso de uso: consultar los cargos con sus advertencias (F03-H05). Refleja {@code listarCargos}.
 */
public interface ConsultarCargos {

    /**
     * Los cargos por docente, cargo y fecha de designación, con sus advertencias de hoy.
     *
     * @param estado solo los de ese estado; o {@code null}
     * @param docente solo los de ese docente; o {@code null}
     */
    List<CargoConsultado> listar(EstadoDeCargo estado, String docente);
}
