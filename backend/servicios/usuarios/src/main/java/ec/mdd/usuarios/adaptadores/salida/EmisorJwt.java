package ec.mdd.usuarios.adaptadores.salida;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * Firma tokens JWT RS256 con la llave privada de Usuarios (ADR-014).
 *
 * <p>El token lleva {@code iss}, {@code sub} (nombre de usuario), {@code roles} ordenados, {@code
 * nombre} (nombre para mostrar), {@code inicio} (segundos desde la época, el instante del inicio de
 * sesión, que se conserva al renovar; ADR-020), {@code iat} y {@code exp}; el encabezado lleva el
 * {@code kid} de la llave, para que quien valida elija la llave pública correcta del JWKS.
 *
 * <p>El token de proceso usa la misma llave y el mismo formato, con {@code sub} = {@code
 * proceso:<proceso>}, {@code roles} = {@code [PROCESO]} y su propia vigencia (D-49, ADR-018).
 */
public final class EmisorJwt implements EmisorDeTokens {

    private final JwtEncoder codificador;
    private final Clock reloj;
    private final Duration vigencia;
    private final Duration vigenciaProceso;
    private final String emisor;
    private final String kid;

    /**
     * @param codificador codificador con la llave privada
     * @param reloj reloj para {@code iat} y {@code exp}
     * @param vigencia cuánto dura el token de un usuario
     * @param vigenciaProceso cuánto dura el token de un proceso
     * @param emisor valor de {@code iss}
     * @param kid identificador de la llave en el JWKS
     */
    public EmisorJwt(
            JwtEncoder codificador,
            Clock reloj,
            Duration vigencia,
            Duration vigenciaProceso,
            String emisor,
            String kid) {
        this.codificador = codificador;
        this.reloj = reloj;
        this.vigencia = vigencia;
        this.vigenciaProceso = vigenciaProceso;
        this.emisor = emisor;
        this.kid = kid;
    }

    @Override
    public TokenEmitido emitir(Usuario usuario, Instant inicio) {
        return firmar(
                usuario.nombreUsuario(),
                usuario.roles().stream().sorted().toList(),
                Map.of("nombre", usuario.nombreParaMostrar(), "inicio", inicio.getEpochSecond()),
                vigencia);
    }

    @Override
    public TokenEmitido emitirParaProceso(String proceso) {
        return firmar("proceso:" + proceso, List.of("PROCESO"), Map.of(), vigenciaProceso);
    }

    private TokenEmitido firmar(
            String sujeto, List<String> roles, Map<String, Object> extra, Duration duracion) {
        Instant ahora = reloj.instant();
        Instant vence = ahora.plus(duracion);
        JwtClaimsSet reclamos =
                JwtClaimsSet.builder()
                        .issuer(emisor)
                        .subject(sujeto)
                        .claim("roles", roles)
                        .issuedAt(ahora)
                        .expiresAt(vence)
                        .claims(reclamosExtra -> reclamosExtra.putAll(extra))
                        .build();
        JwsHeader encabezado = JwsHeader.with(SignatureAlgorithm.RS256).keyId(kid).build();
        String valor =
                codificador.encode(JwtEncoderParameters.from(encabezado, reclamos)).getTokenValue();
        return new TokenEmitido(valor, vence);
    }
}
