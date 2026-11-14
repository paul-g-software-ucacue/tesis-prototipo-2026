package ec.mdd.validacionocs.puertos.salida;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Avisa que el distributivo entró en CONGELADO_EN_FIRMA, en la transacción que registra el paso
 * (F13-H02, D-101). Después de confirmar, Aprobación le entrega la cadena a Firma (MD-05).
 */
@FunctionalInterface
public interface AvisoDeFirma {

    /**
     * Guarda el aviso en la transacción en curso: si la transacción no confirma, no sale.
     *
     * @param aviso la versión, su hash, la cadena y el rol de la etapa
     */
    void avisar(DistributivoEnFirma aviso);

    /**
     * El distributivo pasó a CONGELADO_EN_FIRMA con su versión congelada.
     *
     * @param periodo el código del periodo
     * @param version la versión congelada
     * @param hash su hash de contenido (D-100)
     * @param cadena la cadena de firmas del consolidado de la copia de la configuración, en orden
     * @param rolDeLaEtapa el rol de CONGELADO_EN_FIRMA en la copia
     * @param momento cuándo entró en firma
     */
    record DistributivoEnFirma(
            String periodo,
            int version,
            String hash,
            List<Paso> cadena,
            String rolDeLaEtapa,
            Instant momento) {

        /** Exige los datos y copia la cadena. */
        public DistributivoEnFirma {
            Objects.requireNonNull(periodo, "periodo");
            Objects.requireNonNull(hash, "hash");
            Objects.requireNonNull(rolDeLaEtapa, "rolDeLaEtapa");
            Objects.requireNonNull(momento, "momento");
            cadena = List.copyOf(cadena);
        }
    }

    /**
     * Un paso de la cadena.
     *
     * @param orden su lugar, desde 1
     * @param etiqueta Elaborado, Revisado o Aprobado
     * @param rol el rol que firma
     */
    record Paso(int orden, String etiqueta, String rol) {}
}
