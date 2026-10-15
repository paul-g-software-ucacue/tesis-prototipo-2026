package ec.mdd.plataforma.eventos;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Clientes HTTP con plazo de espera para las llamadas entre procesos (principio V). */
final class Clientes {

    private Clientes() {}

    /**
     * Un {@link RestClient} sobre el {@link HttpClient} del JDK, en HTTP/1.1, con el mismo plazo
     * para conectar y para esperar la respuesta.
     */
    static RestClient http(Duration plazo) {
        JdkClientHttpRequestFactory fabrica =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                // HTTP/1.1 sin intentar el upgrade a h2c: los procesos atienden
                                // HTTP plano dentro de la red de Docker.
                                .version(HttpClient.Version.HTTP_1_1)
                                .connectTimeout(plazo)
                                .build());
        fabrica.setReadTimeout(plazo);
        return RestClient.builder().requestFactory(fabrica).build();
    }
}
