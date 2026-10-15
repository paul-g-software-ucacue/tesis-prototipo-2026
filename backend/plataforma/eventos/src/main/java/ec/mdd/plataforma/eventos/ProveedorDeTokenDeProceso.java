package ec.mdd.plataforma.eventos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * El token de proceso con el que el emisor se autentica ante el receptor (D-49, ADR-018).
 *
 * <p>Lo pide a Usuarios ({@code POST /interno/usuarios/token-proceso}) con la clave del proceso, lo
 * guarda en memoria y lo renueva cuando le queda menos de un minuto. Si el receptor lo rechaza (por
 * ejemplo, porque identidad se reinició con otra llave), el cliente lo descarta y pide otro. Nunca
 * registra la clave ni el token.
 */
public final class ProveedorDeTokenDeProceso {

    private static final Duration MARGEN = Duration.ofMinutes(1);

    private final RestClient cliente;
    private final URI url;
    private final String proceso;
    private final String clave;
    private final Clock reloj;
    private Vigente actual;

    /**
     * @param cliente cliente HTTP con plazo de espera
     * @param url URL del token de proceso de Usuarios
     * @param proceso nombre de este proceso
     * @param clave clave del proceso; vacía, no se pide el token y la entrega falla
     * @param reloj para decidir cuándo renovarlo
     */
    public ProveedorDeTokenDeProceso(
            RestClient cliente, URI url, String proceso, String clave, Clock reloj) {
        this.cliente = cliente;
        this.url = url;
        this.proceso = proceso;
        this.clave = clave;
        this.reloj = reloj;
    }

    /**
     * El token vigente, o uno nuevo si no hay o está por vencer.
     *
     * @return el token, sin el prefijo {@code Bearer}
     * @throws EntregaFallida si no hay clave o Usuarios no emite el token
     */
    public synchronized String token() {
        if (clave == null || clave.isBlank()) {
            throw new EntregaFallida("sin clave de proceso para " + proceso);
        }
        if (actual == null || !sirve(actual.venceEn(), reloj)) {
            actual = pedir();
        }
        return actual.token();
    }

    /** Olvida el token guardado: el próximo {@link #token()} pide otro. */
    public synchronized void descartar() {
        actual = null;
    }

    // Sirve si vence después de ahora más el margen.
    static boolean sirve(Instant venceEn, Clock reloj) {
        return venceEn.isAfter(reloj.instant().plus(MARGEN));
    }

    private Vigente pedir() {
        if (url == null) {
            throw new EntregaFallida("sin URL del token de proceso");
        }
        try {
            RespuestaToken respuesta =
                    cliente.post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(new Credenciales(proceso, clave))
                            .exchange(
                                    (peticion, resultado) -> {
                                        int estado = resultado.getStatusCode().value();
                                        if (estado != 200) {
                                            throw new EntregaFallida(
                                                    "Usuarios respondió "
                                                            + estado
                                                            + " al pedir el token de proceso");
                                        }
                                        return resultado.bodyTo(RespuestaToken.class);
                                    });
            if (respuesta == null || respuesta.token() == null || respuesta.venceEn() == null) {
                throw new EntregaFallida("Usuarios respondió sin token de proceso");
            }
            return new Vigente(respuesta.token(), respuesta.venceEn());
        } catch (RestClientException e) {
            throw new EntregaFallida(
                    "no se pudo pedir el token de proceso: "
                            + e.getMostSpecificCause().getClass().getSimpleName());
        }
    }

    // Lo que se envía a Usuarios; toString sin la clave.
    record Credenciales(String proceso, String clave) {

        @Override
        public String toString() {
            return "Credenciales[proceso=" + proceso + "]";
        }
    }

    // Lo que responde Usuarios (TokenSesion); el tipo siempre es Bearer.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaToken(String token, Instant venceEn) {

        @Override
        public String toString() {
            return "RespuestaToken[venceEn=" + venceEn + "]";
        }
    }

    private record Vigente(String token, Instant venceEn) {

        @Override
        public String toString() {
            return "Vigente[venceEn=" + venceEn + "]";
        }
    }
}
