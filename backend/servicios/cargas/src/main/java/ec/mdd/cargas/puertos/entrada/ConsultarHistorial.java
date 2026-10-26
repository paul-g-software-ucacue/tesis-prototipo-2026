package ec.mdd.cargas.puertos.entrada;

import ec.mdd.cargas.dominio.EntradaDeHistorial;
import java.util.List;

/**
 * Caso de uso: el historial de una carga (RF-10, F01-H04). Refleja la operación {@code
 * listarHistorial} del contrato de Cargas.
 */
public interface ConsultarHistorial {

    /**
     * Los intentos de confirmar la carga, del más reciente al más antiguo.
     *
     * @throws CargaNoEncontrada si no es una de las 10
     */
    List<EntradaDeHistorial> historial(String codigo);
}
