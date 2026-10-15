package ec.mdd.plataforma.eventos;

import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Entrega un evento al receptor de su destino por HTTP, con plazo de espera y el token de proceso
 * (principio V, ADR-018).
 *
 * <p>Solo una respuesta 2xx confirma la entrega. Un 401 descarta el token, pide otro y reintenta
 * una sola vez: identidad pudo reiniciarse con otra llave (ADR-014). Cualquier otra respuesta, un
 * plazo vencido o una conexión rechazada lanzan {@link EntregaFallida}.
 */
public final class ClienteDeEventos {

    private final RestClient cliente;
    private final Map<String, URI> destinos;
    private final ProveedorDeTokenDeProceso tokens;

    /**
     * @param cliente cliente HTTP con plazo de espera
     * @param destinos URL del receptor de cada servicio destino
     * @param tokens el token de proceso
     */
    public ClienteDeEventos(
            RestClient cliente, Map<String, URI> destinos, ProveedorDeTokenDeProceso tokens) {
        this.cliente = cliente;
        this.destinos = Map.copyOf(destinos);
        this.tokens = tokens;
    }

    /**
     * Entrega el evento y espera la confirmación del receptor.
     *
     * @param evento el sobre guardado en el registro
     * @throws EntregaFallida si el receptor no confirma
     */
    public void entregar(EventoEntreProcesos evento) {
        URI url = destinos.get(evento.destino());
        if (url == null) {
            throw new EntregaFallida("sin URL configurada para el destino " + evento.destino());
        }
        CuerpoEvento cuerpo = CuerpoEvento.de(evento);
        int estado = enviar(url, cuerpo, tokens.token());
        if (estado == 401) {
            tokens.descartar();
            estado = enviar(url, cuerpo, tokens.token());
        }
        if (estado < 200 || estado >= 300) {
            throw new EntregaFallida("el receptor respondió " + estado);
        }
    }

    private int enviar(URI url, CuerpoEvento cuerpo, String token) {
        try {
            Integer estado =
                    cliente.post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .body(cuerpo)
                            .exchange((peticion, respuesta) -> respuesta.getStatusCode().value());
            return estado == null ? 0 : estado;
        } catch (RestClientException e) {
            throw new EntregaFallida(
                    "no se pudo conectar con el receptor: "
                            + e.getMostSpecificCause().getClass().getSimpleName());
        }
    }
}
