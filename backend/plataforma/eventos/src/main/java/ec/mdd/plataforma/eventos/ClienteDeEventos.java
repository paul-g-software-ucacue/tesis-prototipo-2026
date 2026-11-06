package ec.mdd.plataforma.eventos;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Entrega un evento al receptor de su destino: a un {@link ReceptorLocal} si el destino está en el
 * mismo proceso (ADR-024), y si no, por HTTP con plazo de espera y el token de proceso (principio
 * V, ADR-018).
 *
 * <p>Por HTTP, solo una respuesta 2xx confirma la entrega. Un 401 descarta el token, pide otro y
 * reintenta una sola vez: identidad pudo reiniciarse con otra llave (ADR-014). Cualquier otra
 * respuesta, un plazo vencido, una conexión rechazada o una excepción del receptor local lanzan
 * {@link EntregaFallida}.
 */
public final class ClienteDeEventos {

    private final RestClient cliente;
    private final Map<String, URI> destinos;
    private final ProveedorDeTokenDeProceso tokens;
    private final Map<String, ReceptorLocal> locales;

    /**
     * Cliente que solo entrega por HTTP.
     *
     * @param cliente cliente HTTP con plazo de espera
     * @param destinos URL del receptor de cada servicio destino
     * @param tokens el token de proceso
     */
    public ClienteDeEventos(
            RestClient cliente, Map<String, URI> destinos, ProveedorDeTokenDeProceso tokens) {
        this(cliente, destinos, tokens, List.of());
    }

    /**
     * Cliente que entrega a los receptores del mismo proceso y, para los demás destinos, por HTTP.
     *
     * @param cliente cliente HTTP con plazo de espera
     * @param destinos URL del receptor de cada servicio destino de otro proceso
     * @param tokens el token de proceso
     * @param locales los receptores del mismo proceso, uno por destino
     * @throws IllegalArgumentException si dos receptores locales atienden el mismo destino
     */
    public ClienteDeEventos(
            RestClient cliente,
            Map<String, URI> destinos,
            ProveedorDeTokenDeProceso tokens,
            List<ReceptorLocal> locales) {
        this.cliente = cliente;
        this.destinos = Map.copyOf(destinos);
        this.tokens = tokens;
        this.locales = porDestino(locales);
    }

    /**
     * Entrega el evento y espera la confirmación del receptor.
     *
     * @param evento el sobre guardado en el registro
     * @throws EntregaFallida si el receptor no confirma
     */
    public void entregar(EventoEntreProcesos evento) {
        ReceptorLocal local = locales.get(evento.destino());
        if (local != null) {
            entregarLocal(local, evento);
            return;
        }
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

    // El mensaje solo lleva la clase de la causa: el del receptor puede citar el contenido.
    private static void entregarLocal(ReceptorLocal local, EventoEntreProcesos evento) {
        try {
            local.recibir(evento);
        } catch (RuntimeException e) {
            throw new EntregaFallida(
                    "el receptor local de "
                            + evento.destino()
                            + " no lo registró: "
                            + e.getClass().getSimpleName());
        }
    }

    private static Map<String, ReceptorLocal> porDestino(List<ReceptorLocal> locales) {
        Map<String, ReceptorLocal> porDestino = new HashMap<>();
        for (ReceptorLocal local : locales) {
            if (porDestino.putIfAbsent(local.destino(), local) != null) {
                throw new IllegalArgumentException(
                        "Hay dos receptores locales para el destino " + local.destino());
            }
        }
        return Map.copyOf(porDestino);
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
