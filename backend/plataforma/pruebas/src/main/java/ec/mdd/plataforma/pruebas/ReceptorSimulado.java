package ec.mdd.plataforma.pruebas;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Receptor de eventos y emisor de tokens de proceso simulados, con el servidor HTTP del JDK, para
 * probar la entrega entre procesos sin levantar Usuarios (ADR-018).
 *
 * <p>Atiende las dos rutas de Usuarios que usa el emisor: {@code /interno/usuarios/token-proceso},
 * que devuelve {@code token-1}, {@code token-2}… con 5 minutos de vigencia, y {@code
 * /interno/usuarios/eventos}, que responde 204 salvo que se le pida otra cosa. Guarda cada petición
 * de eventos para comprobarla. Se puede detener y volver a levantar en el mismo puerto.
 *
 * <p>Cualquier otra ruta es una consulta entre procesos (ADR-023): responde lo que se le programó
 * con {@link #responderConsulta}, en orden, y 404 si no le queda nada.
 */
public final class ReceptorSimulado implements AutoCloseable {

    /** Ruta del token de proceso. */
    public static final String RUTA_TOKEN = "/interno/usuarios/token-proceso";

    /** Ruta del receptor de eventos. */
    public static final String RUTA_EVENTOS = "/interno/usuarios/eventos";

    /**
     * Una petición recibida por el receptor de eventos.
     *
     * @param autorizacion el encabezado {@code Authorization}, o vacío
     * @param cuerpo el cuerpo JSON
     */
    public record Peticion(String autorizacion, String cuerpo) {}

    private record Respuesta(int estado, String cuerpo) {}

    private final List<Peticion> eventos = new CopyOnWriteArrayList<>();
    private final List<Peticion> consultas = new CopyOnWriteArrayList<>();
    private final Map<String, Deque<Respuesta>> respuestas = new HashMap<>();
    private final Deque<Integer> estadosEventos = new ArrayDeque<>();
    private final Deque<Integer> estadosToken = new ArrayDeque<>();
    private final AtomicInteger tokens = new AtomicInteger();
    private volatile Duration retraso = Duration.ZERO;
    private int puerto;
    private HttpServer servidor;

    /** Levanta el receptor en un puerto libre de la interfaz local. */
    public ReceptorSimulado() {
        levantar();
    }

    /** Vuelve a levantar el receptor, en el mismo puerto si ya tenía uno. */
    public synchronized void levantar() {
        try {
            servidor =
                    HttpServer.create(
                            new InetSocketAddress(InetAddress.getLoopbackAddress(), puerto), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        servidor.createContext(RUTA_TOKEN, this::atenderToken);
        servidor.createContext(RUTA_EVENTOS, this::atenderEvento);
        servidor.createContext("/", this::atenderConsulta);
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.start();
        puerto = servidor.getAddress().getPort();
    }

    /** Detiene el receptor: las conexiones siguientes se rechazan. */
    public synchronized void detener() {
        servidor.stop(0);
    }

    /** URL base, por ejemplo {@code http://127.0.0.1:41234}. */
    public URI base() {
        return URI.create("http://127.0.0.1:" + puerto);
    }

    /** URL del receptor de eventos. */
    public URI urlEventos() {
        return base().resolve(RUTA_EVENTOS);
    }

    /** URL del token de proceso. */
    public URI urlToken() {
        return base().resolve(RUTA_TOKEN);
    }

    /** Las próximas respuestas del receptor de eventos, en orden; después, 204. */
    public synchronized void responderEventos(int... estados) {
        for (int estado : estados) {
            estadosEventos.add(estado);
        }
    }

    /** Las próximas respuestas del token de proceso, en orden; después, 200. */
    public synchronized void responderToken(int... estados) {
        for (int estado : estados) {
            estadosToken.add(estado);
        }
    }

    /**
     * La próxima respuesta de una consulta, después de las que ya tenga programadas.
     *
     * @param ruta la ruta, sin la parte de la consulta
     * @param estado el estado HTTP
     * @param cuerpo el cuerpo JSON, o vacío
     */
    public synchronized void responderConsulta(String ruta, int estado, String cuerpo) {
        respuestas
                .computeIfAbsent(ruta, clave -> new ArrayDeque<>())
                .add(new Respuesta(estado, cuerpo));
    }

    /** Cuánto espera el receptor antes de responder, para probar el plazo. */
    public void retrasar(Duration espera) {
        this.retraso = espera;
    }

    /** Las peticiones que recibió el receptor de eventos. */
    public List<Peticion> eventosRecibidos() {
        return List.copyOf(eventos);
    }

    /** Las consultas que recibió, con su encabezado {@code Authorization}. */
    public List<Peticion> consultasRecibidas() {
        return List.copyOf(consultas);
    }

    /** Cuántos tokens emitió. */
    public int tokensEmitidos() {
        return tokens.get();
    }

    @Override
    public void close() {
        detener();
    }

    private void atenderToken(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            intercambio.getRequestBody().readAllBytes();
            int estado = siguiente(estadosToken, 200);
            if (estado != 200) {
                intercambio.sendResponseHeaders(estado, -1);
                return;
            }
            String cuerpo =
                    "{\"token\":\"token-"
                            + tokens.incrementAndGet()
                            + "\",\"tipo\":\"Bearer\",\"venceEn\":\""
                            + Instant.now().plus(Duration.ofMinutes(5))
                            + "\"}";
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, bytes.length);
            intercambio.getResponseBody().write(bytes);
        }
    }

    private void atenderEvento(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            String cuerpo =
                    new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String autorizacion = intercambio.getRequestHeaders().getFirst("Authorization");
            esperar();
            eventos.add(new Peticion(autorizacion == null ? "" : autorizacion, cuerpo));
            intercambio.sendResponseHeaders(siguiente(estadosEventos, 204), -1);
        }
    }

    private void atenderConsulta(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            intercambio.getRequestBody().readAllBytes();
            String autorizacion = intercambio.getRequestHeaders().getFirst("Authorization");
            consultas.add(new Peticion(autorizacion == null ? "" : autorizacion, ""));
            Respuesta respuesta = siguiente(intercambio.getRequestURI().getPath());
            esperar();
            byte[] bytes = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
            if (bytes.length == 0) {
                intercambio.sendResponseHeaders(respuesta.estado(), -1);
                return;
            }
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(respuesta.estado(), bytes.length);
            intercambio.getResponseBody().write(bytes);
        }
    }

    private synchronized Respuesta siguiente(String ruta) {
        Deque<Respuesta> programadas = respuestas.get(ruta);
        Respuesta respuesta = programadas == null ? null : programadas.poll();
        return respuesta == null ? new Respuesta(404, "") : respuesta;
    }

    private synchronized int siguiente(Deque<Integer> estados, int porDefecto) {
        Integer estado = estados.poll();
        return estado == null ? porDefecto : estado;
    }

    private void esperar() {
        if (!retraso.isZero()) {
            try {
                Thread.sleep(retraso);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
