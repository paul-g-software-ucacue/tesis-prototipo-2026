package ec.mdd.plataforma.pruebas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ReceptorSimuladoTest {

    private final HttpClient cliente =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    @Test
    void dadoElReceptor_cuandoRecibeTokensYEventos_entoncesRespondeYLosGuarda() throws Exception {
        try (ReceptorSimulado receptor = new ReceptorSimulado()) {
            receptor.responderEventos(500);
            receptor.responderToken(503);

            assertThat(enviar(receptor.urlToken(), "").statusCode()).isEqualTo(503);
            HttpResponse<String> token = enviar(receptor.urlToken(), "");
            assertThat(token.statusCode()).isEqualTo(200);
            assertThat(token.body()).contains("\"token\":\"token-1\"");
            assertThat(enviar(receptor.urlEventos(), "{\"a\":1}").statusCode()).isEqualTo(500);
            assertThat(enviar(receptor.urlEventos(), "{\"a\":2}").statusCode()).isEqualTo(204);

            assertThat(receptor.tokensEmitidos()).isEqualTo(1);
            assertThat(receptor.eventosRecibidos())
                    .extracting(ReceptorSimulado.Peticion::cuerpo)
                    .containsExactly("{\"a\":1}", "{\"a\":2}");
            assertThat(receptor.eventosRecibidos().getFirst().autorizacion()).isEqualTo("Bearer x");
        }
    }

    @Test
    void dadoElReceptorDetenido_cuandoVuelveALevantarse_entoncesUsaElMismoPuerto()
            throws Exception {
        try (ReceptorSimulado receptor = new ReceptorSimulado()) {
            URI url = receptor.urlEventos();
            receptor.detener();
            assertThatThrownBy(() -> enviar(url, "{}")).isInstanceOf(IOException.class);

            receptor.levantar();
            receptor.retrasar(Duration.ofMillis(10));

            assertThat(receptor.urlEventos()).isEqualTo(url);
            assertThat(enviar(url, "{}").statusCode()).isEqualTo(204);
            assertThat(receptor.base().toString()).startsWith("http://127.0.0.1:");
        }
    }

    private HttpResponse<String> enviar(URI url, String cuerpo)
            throws IOException, InterruptedException {
        return cliente.send(
                HttpRequest.newBuilder(url)
                        .header("Authorization", "Bearer x")
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
