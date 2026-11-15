package ec.mdd.firma.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * Las firmas de un documento quedaron anuladas por orden de Aprobación, porque la versión firmada
 * se reemplazó (F12-H04, D-106, glosario «Anular firmas»). Las firmas, sus archivos y sus
 * validaciones no cambian: se conservan en el historial, anuladas porque el documento lo está.
 *
 * @param motivo por qué, el nombre del motivo de la API publicada
 * @param firmasAnuladas cuántas firmas aceptadas tenía el documento
 * @param anuladaPor quién la registró: el sistema
 * @param anuladaEn cuándo
 */
public record AnulacionDeFirmas(
        String motivo, int firmasAnuladas, String anuladaPor, Instant anuladaEn) {

    /** Exige los datos y un número de firmas desde 0. */
    public AnulacionDeFirmas {
        Objects.requireNonNull(anuladaPor, "anuladaPor");
        Objects.requireNonNull(anuladaEn, "anuladaEn");
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("La anulación lleva su motivo");
        }
        if (firmasAnuladas < 0) {
            throw new IllegalArgumentException("Las firmas anuladas empiezan en 0");
        }
    }
}
