package ec.mdd.gateway;

import java.time.Duration;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;

/**
 * El API Gateway guarda la respuesta del DNS un segundo como mucho, no lo que diga su TTL (#274,
 * D-116).
 *
 * <p>El cliente de Reactor Netty usa su propio resolvedor y guarda cada respuesta lo que dice su
 * TTL; el DNS interno de Docker responde con 600 s. Si un contenedor se reinicia y otro se queda
 * con su IP (por ejemplo, {@code identidad} se detiene y {@code distributivo} arranca en su
 * dirección), el API Gateway seguía mandando a la IP vieja el tráfico del primero durante hasta
 * diez minutos: el inicio de sesión llegaba a {@code distributivo} y respondía 401. Con la caché de
 * un segundo, el mínimo que admite Netty, la conexión nueva se abre con la IP actual; resolver un
 * nombre dentro de la red de Docker cuesta menos de un milisegundo.
 */
@Configuration(proxyBeanMethods = false)
class ResolucionDeNombres {

    // Netty no admite 0: como mucho un segundo, el mínimo.
    private static final Duration UN_SEGUNDO = Duration.ofSeconds(1);

    /** El cliente HTTP de las rutas, con la caché de DNS de un segundo. */
    @Bean
    HttpClientCustomizer resolucionConCacheCorta() {
        return ResolucionDeNombres::conCacheCorta;
    }

    /**
     * El mismo resolvedor, también para pedir el JWKS a {@code identidad}.
     *
     * @param cliente el cliente de Reactor Netty
     * @return el cliente que guarda las respuestas del DNS un segundo como mucho
     */
    static HttpClient conCacheCorta(HttpClient cliente) {
        return cliente.resolver(
                resolvedor ->
                        resolvedor
                                .cacheMaxTimeToLive(UN_SEGUNDO)
                                .cacheNegativeTimeToLive(UN_SEGUNDO));
    }
}
