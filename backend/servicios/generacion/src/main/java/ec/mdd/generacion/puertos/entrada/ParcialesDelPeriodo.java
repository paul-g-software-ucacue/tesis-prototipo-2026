package ec.mdd.generacion.puertos.entrada;

import java.util.List;

/**
 * Los parciales de un periodo.
 *
 * @param creado si el periodo tiene distributivo
 * @param parciales uno por carrera; vacía sin distributivo
 */
public record ParcialesDelPeriodo(boolean creado, List<ParcialConAvance> parciales) {

    /** Copia la lista. */
    public ParcialesDelPeriodo {
        parciales = List.copyOf(parciales);
    }
}
