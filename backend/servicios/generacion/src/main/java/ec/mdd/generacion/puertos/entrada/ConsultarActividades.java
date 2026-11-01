package ec.mdd.generacion.puertos.entrada;

import java.util.Optional;

/** Las actividades no lectivas de un periodo, por carrera y docente (RF-26). */
public interface ConsultarActividades {

    /**
     * Las actividades del periodo.
     *
     * @param periodo el código del periodo
     * @param carrera solo esa carrera, aunque aún no tenga parcial; vacío, todas (D-64)
     * @return las actividades
     */
    ActividadesDelPeriodo actividades(String periodo, Optional<String> carrera);
}
