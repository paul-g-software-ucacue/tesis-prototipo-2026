package ec.mdd.cargas.puertos.entrada;

import ec.mdd.cargas.dominio.EstadoDeCarga;
import java.util.List;

/**
 * Caso de uso: el estado de las 10 cargas (RF-56, F01-H05, glosario «Estado de la carga»). Refleja
 * {@code consultarEstadoDeLasCargas}.
 */
public interface ConsultarEstadoDeLasCargas {

    /**
     * Las 10 cargas en su orden, cada una pendiente, cargada o «sin proyectos», con lo que le
     * falta.
     */
    List<EstadoDeCarga> estados();
}
