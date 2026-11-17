package ec.mdd.validacionocs.puertos.salida;

import java.util.Map;

/**
 * El nombre de cada carrera, para los títulos de las notificaciones (F16-H01, D-108): Aprobación
 * solo guarda el código, y el nombre lo tiene Académico.
 */
@FunctionalInterface
public interface NombresDeCarreras {

    /**
     * @return el nombre de cada carrera, por su código
     * @throws RuntimeException si Académico no responde
     */
    Map<String, String> nombres();
}
