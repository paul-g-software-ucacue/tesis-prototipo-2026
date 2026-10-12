package ec.mdd.arquitectura.fixtures.capas.dominioconhttp.dominio;

import java.net.http.HttpClient;

/** Violación: el dominio usa un cliente HTTP. */
public class ConHttp {
    private final HttpClient cliente = HttpClient.newHttpClient();

    public HttpClient cliente() {
        return cliente;
    }
}
