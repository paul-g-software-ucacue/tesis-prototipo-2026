package ec.mdd.plataforma.pruebas;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class ServidorJwksTest {

    @Test
    void dadoUnServidor_cuandoSePideElJwks_entoncesTraeSoloLaLlavePublica() throws Exception {
        TokensDePrueba tokens = new TokensDePrueba();
        try (ServidorJwks servidor = new ServidorJwks(tokens.jwks());
                HttpClient cliente = HttpClient.newHttpClient()) {
            HttpResponse<String> respuesta =
                    cliente.send(
                            HttpRequest.newBuilder(URI.create(servidor.uri())).build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(respuesta.statusCode()).isEqualTo(200);
            RSAKey llave = (RSAKey) JWKSet.parse(respuesta.body()).getKeyByKeyId("llave-de-prueba");
            assertThat(llave.toRSAPublicKey()).isEqualTo(tokens.llave().toRSAPublicKey());
            assertThat(llave.isPrivate()).isFalse();
            assertThat(respuesta.body()).doesNotContain("\"d\"");
        }
    }
}
