package ec.mdd.plataforma.eventos;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Lee datos de otro proceso por una ruta interna, con el token de proceso y un plazo de espera
 * (ADR-023). Nunca devuelve un resultado parcial: o el dato, o vacío, o una excepción.
 *
 * <p>Ante un 401 pide otro token y reintenta una vez, como {@link ClienteDeEventos}, porque el
 * dueño puede haber rotado la llave.
 */
public final class ConsultasEntreProcesos {

    private static final int OK = 200;
    private static final int NO_AUTENTICADO = 401;
    private static final int NO_ENCONTRADO = 404;

    private final RestClient cliente;
    private final ProveedorDeTokenDeProceso tokens;

    /**
     * @param cliente cliente HTTP con plazo de espera
     * @param tokens el token de proceso
     */
    public ConsultasEntreProcesos(RestClient cliente, ProveedorDeTokenDeProceso tokens) {
        this.cliente = cliente;
        this.tokens = tokens;
    }

    /**
     * Pide un dato a su dueño.
     *
     * @param url la ruta interna
     * @param tipo el tipo del cuerpo
     * @return el dato, o vacío si el dueño responde 404; el adaptador dice qué significa
     * @throws ProcesoConError si el dueño falla o no se puede conectar con él o pedir el token
     * @throws ProcesoSinRespuesta si no responde dentro del plazo
     */
    public <T> Optional<T> obtener(URI url, Class<T> tipo) {
        Respuesta<T> respuesta = pedir(url, tipo, token());
        if (respuesta.estado() == NO_AUTENTICADO) {
            tokens.descartar();
            respuesta = pedir(url, tipo, token());
        }
        return switch (respuesta.estado()) {
            case OK -> Optional.ofNullable(respuesta.cuerpo());
            case NO_ENCONTRADO -> Optional.empty();
            default -> throw new ProcesoConError("el otro proceso respondió " + respuesta.estado());
        };
    }

    private String token() {
        try {
            return tokens.token();
        } catch (EntregaFallida e) {
            throw new ProcesoConError(e.getMessage());
        }
    }

    private <T> Respuesta<T> pedir(URI url, Class<T> tipo, String token) {
        try {
            return cliente.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange(
                            (peticion, respuesta) -> {
                                int estado = respuesta.getStatusCode().value();
                                return new Respuesta<>(
                                        estado, estado == OK ? respuesta.bodyTo(tipo) : null);
                            });
        } catch (RestClientException e) {
            Throwable causa = e.getMostSpecificCause();
            if (causa instanceof HttpTimeoutException || causa instanceof SocketTimeoutException) {
                throw new ProcesoSinRespuesta();
            }
            throw new ProcesoConError(
                    "no se pudo consultar el otro proceso: " + causa.getClass().getSimpleName());
        }
    }

    private record Respuesta<T>(int estado, T cuerpo) {}
}
