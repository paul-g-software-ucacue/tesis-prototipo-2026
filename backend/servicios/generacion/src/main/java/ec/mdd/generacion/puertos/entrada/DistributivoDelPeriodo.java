package ec.mdd.generacion.puertos.entrada;

import java.util.List;

/**
 * El distributivo de un periodo.
 *
 * @param periodo el código del periodo
 * @param version el número de su versión vigente
 * @param carreras las carreras con parcial
 */
public record DistributivoDelPeriodo(String periodo, int version, List<String> carreras) {

    /** Copia la lista. */
    public DistributivoDelPeriodo {
        carreras = List.copyOf(carreras);
    }
}
