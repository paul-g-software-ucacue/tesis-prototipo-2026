package ec.mdd.usuarios.infraestructura;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * La llave RSA con la que Usuarios firma los tokens (ADR-014).
 *
 * <p>Si hay una ruta configurada, carga la llave privada de ese archivo PEM (PKCS#8): es el caso
 * del VPS (B00-H08). Si no, genera una llave efímera de 2048 bits al arrancar y lo avisa en el
 * registro: es el caso de local y de la CI, y reiniciar el proceso invalida los tokens emitidos. El
 * {@code kid} es la huella SHA-256 de la llave pública. La llave privada nunca sale de esta clase
 * hacia un registro ni hacia el JWKS.
 */
public final class LlavesDeFirma {

    private static final Logger REGISTRO = LoggerFactory.getLogger(LlavesDeFirma.class);

    private final RSAKey llave;

    private LlavesDeFirma(RSAKey llave) {
        this.llave = llave;
    }

    /**
     * Carga la llave del archivo o, sin ruta, genera una efímera.
     *
     * @param rutaPem ruta del archivo PEM; vacía o nula para una llave efímera
     */
    public static LlavesDeFirma cargar(String rutaPem) {
        try {
            KeyPair par;
            if (rutaPem == null || rutaPem.isBlank()) {
                REGISTRO.warn(
                        "Sin mdd.usuarios.llave-privada: se usa una llave efímera; reiniciar el"
                                + " proceso invalida los tokens emitidos.");
                KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
                generador.initialize(2048);
                par = generador.generateKeyPair();
            } else {
                par = leerPem(Path.of(rutaPem));
            }
            RSAKey sinKid =
                    new RSAKey.Builder((RSAPublicKey) par.getPublic())
                            .privateKey(par.getPrivate())
                            .build();
            return new LlavesDeFirma(
                    new RSAKey.Builder(sinKid)
                            .keyID(sinKid.computeThumbprint().toString())
                            .keyUse(KeyUse.SIGNATURE)
                            .algorithm(JWSAlgorithm.RS256)
                            .build());
        } catch (GeneralSecurityException | JOSEException e) {
            throw new IllegalStateException("No se pudo preparar la llave de firma", e);
        }
    }

    /** La llave completa, con la parte privada, para firmar. */
    public RSAKey llave() {
        return llave;
    }

    /** JWKS con solo la parte pública. */
    public JWKSet jwksPublico() {
        return new JWKSet(llave.toPublicJWK());
    }

    private static KeyPair leerPem(Path ruta) throws GeneralSecurityException {
        String pem;
        try {
            pem = Files.readString(ruta, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la llave privada", e);
        }
        String base64 = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        KeyFactory fabrica = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKey privada =
                (RSAPrivateCrtKey)
                        fabrica.generatePrivate(
                                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        RSAPublicKey publica =
                (RSAPublicKey)
                        fabrica.generatePublic(
                                new RSAPublicKeySpec(
                                        privada.getModulus(), privada.getPublicExponent()));
        return new KeyPair(publica, privada);
    }
}
