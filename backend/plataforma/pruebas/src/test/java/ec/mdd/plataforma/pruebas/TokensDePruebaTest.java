package ec.mdd.plataforma.pruebas;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TokensDePruebaTest {

    private final TokensDePrueba tokens = new TokensDePrueba();

    @Test
    void dadoUnTokenValido_cuandoSeLee_entoncesEsRs256DelEmisorConSusRoles() throws Exception {
        SignedJWT jwt = SignedJWT.parse(tokens.valido("autor.prueba", List.of("DOCENTE")));

        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(jwt.getHeader().getKeyID()).isEqualTo(tokens.llave().getKeyID());
        assertThat(jwt.verify(new RSASSAVerifier(tokens.llave().toRSAPublicKey()))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo(TokensDePrueba.EMISOR);
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("autor.prueba");
        assertThat(jwt.getJWTClaimsSet().getStringListClaim("roles")).containsExactly("DOCENTE");
        assertThat(jwt.getJWTClaimsSet().getExpirationTime())
                .isAfter(java.util.Date.from(Instant.now()));
    }

    @Test
    void dadoLosTokensInvalidos_cuandoSeLeen_entoncesCadaUnoFallaPorSuMotivo() throws Exception {
        assertThat(SignedJWT.parse(tokens.vencido()).getJWTClaimsSet().getExpirationTime())
                .isBefore(java.util.Date.from(Instant.now().minusSeconds(60)));
        assertThat(
                        SignedJWT.parse(tokens.deOtraLlave())
                                .verify(new RSASSAVerifier(tokens.llave().toRSAPublicKey())))
                .isFalse();
        assertThat(SignedJWT.parse(tokens.deOtroEmisor()).getJWTClaimsSet().getIssuer())
                .isEqualTo("otro-emisor");
        assertThat(SignedJWT.parse(tokens.hs256()).getHeader().getAlgorithm())
                .isEqualTo(JWSAlgorithm.HS256);
        JWT sinFirma = JWTParser.parse(tokens.sinAlgoritmo());
        assertThat(sinFirma.getHeader().getAlgorithm().getName()).isEqualTo("none");
        assertThat(
                        SignedJWT.parse(tokens.alterado())
                                .verify(new RSASSAVerifier(tokens.llave().toRSAPublicKey())))
                .isFalse();
    }
}
