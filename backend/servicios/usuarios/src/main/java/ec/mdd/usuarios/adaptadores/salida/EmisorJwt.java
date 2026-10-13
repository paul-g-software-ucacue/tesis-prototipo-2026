package ec.mdd.usuarios.adaptadores.salida;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * Firma tokens JWT RS256 con la llave privada de Usuarios (ADR-014).
 *
 * <p>El token lleva {@code iss}, {@code sub} (nombre de usuario), {@code roles} ordenados, {@code
 * iat} y {@code exp}; el encabezado lleva el {@code kid} de la llave, para que quien valida elija
 * la llave pública correcta del JWKS.
 */
public final class EmisorJwt implements EmisorDeTokens {

    private final JwtEncoder codificador;
    private final Clock reloj;
    private final Duration vigencia;
    private final String emisor;
    private final String kid;

    /**
     * @param codificador codificador con la llave privada
     * @param reloj reloj para {@code iat} y {@code exp}
     * @param vigencia cuánto dura el token
     * @param emisor valor de {@code iss}
     * @param kid identificador de la llave en el JWKS
     */
    public EmisorJwt(
            JwtEncoder codificador, Clock reloj, Duration vigencia, String emisor, String kid) {
        this.codificador = codificador;
        this.reloj = reloj;
        this.vigencia = vigencia;
        this.emisor = emisor;
        this.kid = kid;
    }

    @Override
    public TokenEmitido emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        Instant vence = ahora.plus(vigencia);
        JwtClaimsSet reclamos =
                JwtClaimsSet.builder()
                        .issuer(emisor)
                        .subject(usuario.nombreUsuario())
                        .claim("roles", usuario.roles().stream().sorted().toList())
                        .issuedAt(ahora)
                        .expiresAt(vence)
                        .build();
        JwsHeader encabezado = JwsHeader.with(SignatureAlgorithm.RS256).keyId(kid).build();
        String valor =
                codificador.encode(JwtEncoderParameters.from(encabezado, reclamos)).getTokenValue();
        return new TokenEmitido(valor, vence);
    }
}
