package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.Inconsistencia;
import java.util.Optional;

/**
 * Lo que pasó al proponer: se guardó, o falta la justificación de una regla blanda (RF-28).
 *
 * @param propuesta la propuesta guardada; vacía si no se guardó
 * @param advertencia la inconsistencia sin justificar; vacía si se guardó
 */
public record ResultadoDePropuesta(
        Optional<PropuestaVista> propuesta, Optional<Inconsistencia> advertencia) {

    /** Una propuesta guardada. */
    public static ResultadoDePropuesta guardada(PropuestaVista propuesta) {
        return new ResultadoDePropuesta(Optional.of(propuesta), Optional.empty());
    }

    /** No se guardó: falta la justificación. */
    public static ResultadoDePropuesta conAdvertencia(Inconsistencia advertencia) {
        return new ResultadoDePropuesta(Optional.empty(), Optional.of(advertencia));
    }
}
