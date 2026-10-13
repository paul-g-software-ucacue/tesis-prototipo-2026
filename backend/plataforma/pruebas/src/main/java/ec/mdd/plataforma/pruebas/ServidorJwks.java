package ec.mdd.plataforma.pruebas;

import com.nimbusds.jose.jwk.JWKSet;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Sirve un JWKS por HTTP en un puerto libre de la máquina local, para que las pruebas usen el mismo
 * camino que en el perfil consolidado: el decodificador pide el JWKS por su URI.
 *
 * <p>Solo publica la parte pública de las llaves. Se cierra con {@link #close()}.
 */
public final class ServidorJwks implements AutoCloseable {

    private final HttpServer servidor;

    /**
     * Arranca el servidor.
     *
     * @param jwks llaves que publica; solo se sirve su parte pública
     * @throws IOException si no puede abrir el puerto
     */
    public ServidorJwks(JWKSet jwks) throws IOException {
        byte[] cuerpo = jwks.toString(true).getBytes(StandardCharsets.UTF_8);
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext(
                "/jwks",
                intercambio -> {
                    intercambio.getResponseHeaders().add("Content-Type", "application/json");
                    intercambio.sendResponseHeaders(200, cuerpo.length);
                    try (OutputStream salida = intercambio.getResponseBody()) {
                        salida.write(cuerpo);
                    }
                });
        servidor.start();
    }

    /** URI del JWKS, para {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}. */
    public String uri() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/jwks";
    }

    @Override
    public void close() {
        servidor.stop(0);
    }
}
