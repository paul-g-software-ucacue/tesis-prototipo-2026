package ec.mdd.firma.dominio;

import java.util.Objects;

/**
 * Un paso de la cadena de firmas que entregó Aprobación (MD-05): su orden, su etiqueta y el rol que
 * firma (RF-58).
 *
 * @param orden su lugar, desde 1
 * @param etiqueta Elaborado, Revisado o Aprobado, según la configuración
 * @param rol el rol que firma
 */
public record PasoDeFirma(int orden, String etiqueta, String rol) {

    /** Exige los datos. */
    public PasoDeFirma {
        if (orden < 1) {
            throw new IllegalArgumentException("El orden empieza en 1");
        }
        Objects.requireNonNull(etiqueta, "etiqueta");
        Objects.requireNonNull(rol, "rol");
    }
}
