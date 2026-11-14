package ec.mdd.firma.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * El certificado de la firma nueva, como lo muestra el informe (F14-H02). El nombre no se compara
 * todavía con el firmante esperado, ni la vigencia con el momento de la subida: es de F14-H03.
 *
 * @param nombre el CN del sujeto, o el nombre completo si no tiene CN; hasta 200 caracteres
 * @param emisor el nombre completo del emisor; hasta 400 caracteres
 * @param serie el número de serie en hexadecimal, en minúsculas
 * @param validoDesde el inicio de su vigencia
 * @param validoHasta el fin de su vigencia
 */
public record CertificadoDeLaFirma(
        String nombre, String emisor, String serie, Instant validoDesde, Instant validoHasta) {

    private static final Pattern SERIE = Pattern.compile("^[0-9a-f]{1,64}$");

    /** Exige los datos y sus largos, los del contrato. */
    public CertificadoDeLaFirma {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(emisor, "emisor");
        Objects.requireNonNull(serie, "serie");
        Objects.requireNonNull(validoDesde, "validoDesde");
        Objects.requireNonNull(validoHasta, "validoHasta");
        if (nombre.length() > 200 || emisor.length() > 400) {
            throw new IllegalArgumentException("el nombre o el emisor son demasiado largos");
        }
        if (!SERIE.matcher(serie).matches()) {
            throw new IllegalArgumentException("la serie es hexadecimal en minúsculas");
        }
    }
}
