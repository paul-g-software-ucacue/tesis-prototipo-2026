package ec.mdd.plataforma.eventos;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Clientes HTTP con plazo de espera para las llamadas entre procesos (principio V). */
final class Clientes {

    private Clientes() {}

    /**
     * Un {@link RestClient} sobre el {@link HttpClient} del JDK, con el mismo plazo para conectar y
     * para esperar la respuesta.
     */
    static RestClient http(Duration plazo) {
        JdkClientHttpRequestFactory fabrica =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().connectTimeout(plazo).build());
        fabrica.setReadTimeout(plazo);
        return RestClient.builder().requestFactory(fabrica).build();
    }
}
