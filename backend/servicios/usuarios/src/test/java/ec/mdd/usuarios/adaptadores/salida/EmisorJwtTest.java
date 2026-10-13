package ec.mdd.usuarios.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.SignedJWT;
import ec.mdd.plataforma.pruebas.TokensDePrueba;
import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class EmisorJwtTest {

    private final TokensDePrueba llaves = new TokensDePrueba();
    private final Instant ahora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final EmisorJwt emisor =
            new EmisorJwt(
                    new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(llaves.llave()))),
                    Clock.fixed(ahora, ZoneOffset.UTC),
                    Duration.ofMinutes(30),
                    "mdd-usuarios",
                    llaves.llave().getKeyID());

    @Test
    void dadoUnUsuario_cuandoSeEmiteSuToken_entoncesEsRs256ConSusDatosYVencimiento()
            throws Exception {
        Usuario usuario =
                new Usuario("autor.prueba", "hash", true, Set.of("DOCENTE", "COORDINADOR_CARRERA"));

        TokenEmitido token = emisor.emitir(usuario);

        SignedJWT firmado = SignedJWT.parse(token.valor());
        assertThat(firmado.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(firmado.getHeader().getKeyID()).isEqualTo(llaves.llave().getKeyID());
        Jwt jwt =
                NimbusJwtDecoder.withPublicKey(llaves.llave().toRSAPublicKey())
                        .build()
                        .decode(token.valor());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("mdd-usuarios");
        assertThat(jwt.getSubject()).isEqualTo("autor.prueba");
        assertThat(jwt.getClaimAsStringList("roles"))
                .containsExactly("COORDINADOR_CARRERA", "DOCENTE");
        assertThat(jwt.getIssuedAt()).isEqualTo(ahora);
        assertThat(jwt.getExpiresAt()).isEqualTo(ahora.plus(Duration.ofMinutes(30)));
        assertThat(token.venceEn()).isEqualTo(jwt.getExpiresAt());
        assertThat(token.toString()).doesNotContain(token.valor());
    }

    @Test
    void dadoUnUsuarioSinRoles_cuandoSeEmiteSuToken_entoncesLlevaUnaListaVacia() throws Exception {
        TokenEmitido token = emisor.emitir(new Usuario("sin.roles", "hash", true, Set.of()));

        Jwt jwt =
                NimbusJwtDecoder.withPublicKey(llaves.llave().toRSAPublicKey())
                        .build()
                        .decode(token.valor());
        assertThat(jwt.getClaimAsStringList("roles")).isEmpty();
    }
}
