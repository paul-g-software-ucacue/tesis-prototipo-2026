package ec.mdd.academico.puertos.entrada;

import java.util.List;
import java.util.Optional;

/** Las asignaturas ofertadas de un periodo, para calcular la afinidad (F06-H01, ADR-023). */
public interface ConsultarAsignaturasOfertadas {

    /**
     * Las asignaturas con al menos un paralelo ofertado.
     *
     * @param periodo el código del periodo
     * @param carrera solo las de esta carrera; vacío, todas
     * @return cada asignatura una sola vez; vacío si el periodo no tiene oferta
     * @throws PeriodoNoEncontrado si el periodo no existe
     */
    List<AsignaturaConCampos> delPeriodo(String periodo, Optional<String> carrera);
}
