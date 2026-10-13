package ec.mdd.usuarios.adaptadores.entrada;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import ec.mdd.usuarios.adaptadores.entrada.api.Jwk;
import ec.mdd.usuarios.adaptadores.entrada.api.Jwks;
import ec.mdd.usuarios.adaptadores.entrada.api.JwksApi;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publica la llave pública de Usuarios en formato JWKS, para que el API Gateway y los procesos
 * validen los tokens (ADR-014). Solo publica partes públicas: el {@link JWKSet} que recibe ya es el
 * público, y además se vuelve a filtrar aquí.
 */
@RestController
class JwksControlador implements JwksApi {

    private final JWKSet jwks;

    JwksControlador(JWKSet jwks) {
        this.jwks = jwks.toPublicJWKSet();
    }

    @Override
    public ResponseEntity<Jwks> consultarJwks() {
        List<Jwk> llaves = jwks.getKeys().stream().map(JwksControlador::aContrato).toList();
        return ResponseEntity.ok(new Jwks(llaves));
    }

    private static Jwk aContrato(JWK llave) {
        RSAKey rsa = llave.toRSAKey();
        return new Jwk(
                rsa.getKeyType().getValue(),
                rsa.getKeyID(),
                rsa.getKeyUse().identifier(),
                rsa.getAlgorithm().getName(),
                rsa.getModulus().toString(),
                rsa.getPublicExponent().toString());
    }
}
