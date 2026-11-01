package ec.mdd.generacion.puertos.entrada;

import java.util.List;

/**
 * Las actividades no lectivas de un periodo.
 *
 * @param creado si el periodo tiene distributivo
 * @param carreras una por parcial; vacía sin distributivo
 */
public record ActividadesDelPeriodo(boolean creado, List<ActividadesDeCarrera> carreras) {

    /** Copia la lista. */
    public ActividadesDelPeriodo {
        carreras = List.copyOf(carreras);
    }
}
