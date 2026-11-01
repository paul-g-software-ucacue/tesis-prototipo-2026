package ec.mdd.generacion.puertos.entrada;

import java.util.Optional;

/** Los parciales de un periodo, con su avance y sus propuestas (RF-24). */
public interface ConsultarParciales {

    /**
     * Los parciales del periodo.
     *
     * @param periodo el código del periodo
     * @param carrera solo el de esta carrera; vacío, todos (D-64)
     * @return los parciales
     * @throws PeriodoNoEncontrado si el periodo no existe
     */
    ParcialesDelPeriodo parciales(String periodo, Optional<String> carrera);
}
