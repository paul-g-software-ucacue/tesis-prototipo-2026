package ec.mdd.firma.puertos.entrada;

import java.util.Objects;

/**
 * Un paso de la cadena de firmas que Aprobación entrega con la solicitud (MD-05, F13-H02).
 *
 * @param orden su lugar, desde 1
 * @param etiqueta Elaborado, Revisado o Aprobado, según la configuración
 * @param rol el rol que firma
 */
public record PasoDeLaCadena(int orden, String etiqueta, String rol) {

    /** Exige los datos. */
    public PasoDeLaCadena {
        if (orden < 1) {
            throw new IllegalArgumentException("El orden empieza en 1");
        }
        Objects.requireNonNull(etiqueta, "etiqueta");
        Objects.requireNonNull(rol, "rol");
    }
}
