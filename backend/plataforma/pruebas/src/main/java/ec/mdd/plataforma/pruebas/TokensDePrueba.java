package ec.mdd.plataforma.pruebas;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Llaves y tokens para probar la autenticación sin archivos de llaves (ADR-014).
 *
 * <p>Genera en memoria dos pares RSA de 2048 bits: el «bueno», cuyo JWKS publica {@link #jwks()}, y
 * otro que nadie publica. Con ellos firma tokens válidos y cada tipo de token que la seguridad
 * tiene que rechazar. Nunca escribe una llave en disco, porque Gitleaks rechaza los archivos de
 * llaves en el repositorio.
 */
public final class TokensDePrueba {

    /** Emisor que esperan el API Gateway y los procesos. */
    public static final String EMISOR = "mdd-usuarios";

    private static final Duration VIGENCIA = Duration.ofMinutes(10);

    private static final SecureRandom AZAR = new SecureRandom();

    private final RSAKey llave;
    private final RSAKey otraLlave;

    /** Genera los dos pares de llaves. */
    public TokensDePrueba() {
        this.llave = generar("llave-de-prueba");
        this.otraLlave = generar("otra-llave");
    }

    /** El par de llaves bueno, con su {@code kid}, para firmar fuera de esta clase. */
    public RSAKey llave() {
        return llave;
    }

    /** JWKS con solo la parte pública de la llave buena. */
    public JWKSet jwks() {
        return new JWKSet(llave.toPublicJWK());
    }

    /** Token RS256 válido durante 10 minutos, firmado con la llave buena. */
    public String valido(String usuario, List<String> roles) {
        return firmar(llave, reclamos(usuario, roles, EMISOR, Instant.now().plus(VIGENCIA)));
    }

    /** Token firmado con la llave buena que venció hace 10 minutos, más que la tolerancia. */
    public String vencido() {
        return firmar(
                llave, reclamos("autor.prueba", List.of(), EMISOR, Instant.now().minus(VIGENCIA)));
    }

    /** Token válido en todo, salvo que lo firma una llave que el JWKS no publica. */
    public String deOtraLlave() {
        return firmar(
                otraLlave,
                reclamos("autor.prueba", List.of(), EMISOR, Instant.now().plus(VIGENCIA)));
    }

    /** Token firmado con la llave buena, pero de otro emisor. */
    public String deOtroEmisor() {
        return firmar(
                llave,
                reclamos("autor.prueba", List.of(), "otro-emisor", Instant.now().plus(VIGENCIA)));
    }

    /** Token HS256 con un secreto cualquiera: un algoritmo distinto de RS256. */
    public String hs256() {
        byte[] secreto = new byte[32];
        AZAR.nextBytes(secreto);
        try {
            SignedJWT jwt =
                    new SignedJWT(
                            new JWSHeader(JWSAlgorithm.HS256),
                            reclamos(
                                    "autor.prueba",
                                    List.of(),
                                    EMISOR,
                                    Instant.now().plus(VIGENCIA)));
            jwt.sign(new MACSigner(secreto));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token HS256 de prueba", e);
        }
    }

    /** Token sin firma ({@code alg: none}). */
    public String sinAlgoritmo() {
        return new PlainJWT(
                        reclamos("autor.prueba", List.of(), EMISOR, Instant.now().plus(VIGENCIA)))
                .serialize();
    }

    /** Token válido con un carácter de la firma cambiado. */
    public String alterado() {
        String token = valido("autor.prueba", List.of("DOCENTE"));
        int posicion = token.lastIndexOf('.') + 10;
        char cambiado = token.charAt(posicion) == 'A' ? 'B' : 'A';
        return token.substring(0, posicion) + cambiado + token.substring(posicion + 1);
    }

    private static JWTClaimsSet reclamos(
            String usuario, List<String> roles, String emisor, Instant vence) {
        Instant emitido = vence.minus(VIGENCIA);
        return new JWTClaimsSet.Builder()
                .issuer(emisor)
                .subject(usuario)
                .claim("roles", roles)
                .issueTime(Date.from(emitido))
                .expirationTime(Date.from(vence))
                .build();
    }

    private static String firmar(RSAKey llave, JWTClaimsSet reclamos) {
        try {
            SignedJWT jwt =
                    new SignedJWT(
                            new JWSHeader.Builder(JWSAlgorithm.RS256)
                                    .keyID(llave.getKeyID())
                                    .build(),
                            reclamos);
            jwt.sign(new RSASSASigner(llave));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token de prueba", e);
        }
    }

    private static RSAKey generar(String kid) {
        try {
            return new RSAKeyGenerator(2048).keyID(kid).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo generar la llave de prueba", e);
        }
    }
}
