package ec.mdd.reporting.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * La versión publicada vigente de un periodo, según Aprobación (D-111, D-113): de ella salen los
 * reportes. Durante una reforma sigue siendo la vigente.
 *
 * @param periodo el código del periodo
 * @param version la versión publicada
 * @param hash su hash de contenido, el de la transición a PUBLICADO
 * @param publicadaEn cuándo se publicó
 * @param publicadaPor quién la publicó
 * @param enReforma si hay una reforma en curso
 */
public record Publicacion(
        String periodo,
        int version,
        String hash,
        Instant publicadaEn,
        String publicadaPor,
        boolean enReforma) {

    private static final Pattern HASH = Pattern.compile("^[0-9a-f]{64}$");

    /** Exige los datos, una versión de 1 en adelante y un hash SHA-256 en hexadecimal. */
    public Publicacion {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(publicadaEn, "publicadaEn");
        Objects.requireNonNull(publicadaPor, "publicadaPor");
        if (version < 1) {
            throw new IllegalArgumentException("versión menor que 1");
        }
        if (!HASH.matcher(hash).matches()) {
            throw new IllegalArgumentException("hash que no es SHA-256 en hexadecimal");
        }
    }
}
