package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.Sugerencia;
import java.util.Objects;
import java.util.Optional;

/**
 * La sugerencia, con lo que hace falta para proponer.
 *
 * @param sugerencia los candidatos y los que no alcanzan el mínimo
 * @param distributivoCreado si el periodo ya tiene distributivo
 * @param propuesta la propuesta actual de la asignatura-paralelo, si tiene
 */
public record SugerenciaDelParalelo(
        Sugerencia sugerencia, boolean distributivoCreado, Optional<PropuestaVista> propuesta) {

    /** Exige los datos. */
    public SugerenciaDelParalelo {
        Objects.requireNonNull(sugerencia, "sugerencia");
        Objects.requireNonNull(propuesta, "propuesta");
    }
}
