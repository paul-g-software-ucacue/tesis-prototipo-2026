package ec.mdd.academico.puertos.entrada;

import java.util.List;
import java.util.Optional;

/** Una asignatura-paralelo de la oferta, para sugerir docentes (F06-H02, ADR-023). */
public interface ConsultarParaleloDeLaOferta {

    /**
     * La asignatura-paralelo, en cualquier estado.
     *
     * @param periodo el código del periodo
     * @param asignatura el código de la asignatura
     * @param paralelo el paralelo
     * @return vacío si no está en la oferta del periodo
     * @throws PeriodoNoEncontrado si el periodo no existe
     */
    Optional<ParaleloConCampos> paralelo(String periodo, String asignatura, String paralelo);

    /**
     * Todas las asignaturas-paralelo del periodo, también las retiradas (F06-H03).
     *
     * @param periodo el código del periodo
     * @return vacía si el periodo no tiene oferta
     * @throws PeriodoNoEncontrado si el periodo no existe
     */
    List<ParaleloConCampos> delPeriodo(String periodo);
}
