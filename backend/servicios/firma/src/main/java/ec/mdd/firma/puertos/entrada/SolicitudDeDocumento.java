package ec.mdd.firma.puertos.entrada;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Lo que Aprobación le entrega a Firma al entrar en CONGELADO_EN_FIRMA (MD-05, F13-H02, D-101).
 *
 * @param periodo el código del periodo
 * @param version la versión congelada
 * @param hash su hash de contenido, el de la congelación (D-100)
 * @param cadena los pasos de la cadena de firmas del consolidado, en su orden
 * @param rolDeLaEtapa el rol de la etapa CONGELADO_EN_FIRMA, el que puede reintentar
 * @param momento cuándo entró en firma
 */
public record SolicitudDeDocumento(
        String periodo,
        int version,
        String hash,
        List<PasoDeLaCadena> cadena,
        String rolDeLaEtapa,
        Instant momento) {

    /** Exige los datos y copia la cadena. */
    public SolicitudDeDocumento {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(rolDeLaEtapa, "rolDeLaEtapa");
        Objects.requireNonNull(momento, "momento");
        cadena = List.copyOf(cadena);
        if (cadena.isEmpty()) {
            throw new IllegalArgumentException("La cadena lleva al menos un firmante");
        }
    }
}
