package ec.mdd.firma.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * La firma aceptada de un paso: su resultado de validación aceptado (D-103). El archivo de la firma
 * es el PDF subido, guardado por su SHA-256.
 *
 * @param orden el paso firmado
 * @param sha256 el SHA-256 del archivo resultante
 * @param tamano su tamaño en bytes
 * @param subidoPor quién lo subió
 * @param subidoEn cuándo
 */
public record FirmaRegistrada(
        int orden, String sha256, int tamano, String subidoPor, Instant subidoEn) {

    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    /** Exige los datos. */
    public FirmaRegistrada {
        Objects.requireNonNull(sha256, "sha256");
        Objects.requireNonNull(subidoPor, "subidoPor");
        Objects.requireNonNull(subidoEn, "subidoEn");
        if (orden < 1 || tamano < 1 || !SHA256.matcher(sha256).matches()) {
            throw new IllegalArgumentException("una firma tiene orden, tamaño y SHA-256");
        }
    }

    /** El archivo de la firma en el almacén, nombrado por su SHA-256 (D-101). */
    public ArchivoDelDocumento archivo() {
        return new ArchivoDelDocumento("documentos/" + sha256 + ".pdf", sha256);
    }
}
