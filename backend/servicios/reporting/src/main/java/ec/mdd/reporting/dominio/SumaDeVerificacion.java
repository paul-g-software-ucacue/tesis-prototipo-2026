package ec.mdd.reporting.dominio;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * La suma de verificación del CSV para SIGA entregado (RF-53, F19-H02, D-120): el SHA-256 de sus
 * bytes exactos, en hexadecimal en minúsculas, el mismo algoritmo del hash del glosario. Es lo que
 * imprimen {@code sha256sum} y {@code certutil}, así que cualquiera la puede comparar a simple
 * vista con el archivo que recibió.
 *
 * @param valor 64 dígitos hexadecimales en minúsculas
 */
public record SumaDeVerificacion(String valor) {

    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    /** Exige 64 dígitos hexadecimales en minúsculas. */
    public SumaDeVerificacion {
        Objects.requireNonNull(valor, "valor");
        if (!SHA256.matcher(valor).matches()) {
            throw new IllegalArgumentException("suma que no es SHA-256 en hexadecimal minúscula");
        }
    }

    /**
     * @param contenido los bytes que se entregan
     * @return su SHA-256
     */
    public static SumaDeVerificacion de(byte[] contenido) {
        try {
            return new SumaDeVerificacion(
                    HexFormat.of()
                            .formatHex(MessageDigest.getInstance("SHA-256").digest(contenido)));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("la JVM no tiene SHA-256", imposible);
        }
    }
}
