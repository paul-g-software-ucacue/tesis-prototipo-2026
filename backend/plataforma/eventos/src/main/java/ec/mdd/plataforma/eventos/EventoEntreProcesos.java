package ec.mdd.plataforma.eventos;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Evento que un servicio publica en su transacción para otro proceso (principio V, ADR-018).
 *
 * <p>El registro persistente de publicaciones lo guarda completo, también el {@code destino}, que
 * cada reenvío necesita para saber a dónde ir. Por HTTP viaja sin el destino, con la forma de
 * {@code EventoEntreProcesos} en {@code contratos/comun/eventos.yaml} ({@link CuerpoEvento}).
 *
 * @param idEvento identificador único (UUID v4); no cambia en los reenvíos y es la clave de
 *     idempotencia del receptor
 * @param tipo {@code <servicio>.<NombreDelEvento>}, por ejemplo {@code generacion.EventoDePrueba}
 * @param origen el servicio que lo publica
 * @param destino el servicio receptor; elige la URL de {@code mdd.eventos.destinos}
 * @param momento instante de la publicación
 * @param contenido datos del evento según su tipo; nunca contraseñas ni tokens
 */
public record EventoEntreProcesos(
        UUID idEvento,
        String tipo,
        String origen,
        String destino,
        Instant momento,
        Map<String, Object> contenido) {

    private static final Pattern TIPO = Pattern.compile("^[a-z]+\\.[A-Z][A-Za-z]+$");
    private static final Pattern SERVICIO = Pattern.compile("^[a-z]+$");

    /** Valida la forma del sobre; el contenido queda como una copia que no se puede modificar. */
    public EventoEntreProcesos {
        Objects.requireNonNull(idEvento, "idEvento");
        Objects.requireNonNull(momento, "momento");
        validar("tipo", tipo, TIPO);
        validar("origen", origen, SERVICIO);
        validar("destino", destino, SERVICIO);
        contenido =
                contenido == null
                        ? Map.of()
                        : Collections.unmodifiableMap(new LinkedHashMap<>(contenido));
    }

    /**
     * Crea un evento nuevo, con un identificador nuevo y el momento del reloj.
     *
     * @param tipo {@code <servicio>.<NombreDelEvento>}
     * @param origen el servicio que lo publica
     * @param destino el servicio receptor
     * @param contenido datos del evento
     * @param reloj de dónde sale el momento
     * @return el evento listo para publicar
     */
    public static EventoEntreProcesos nuevo(
            String tipo,
            String origen,
            String destino,
            Map<String, Object> contenido,
            Clock reloj) {
        return new EventoEntreProcesos(
                UUID.randomUUID(), tipo, origen, destino, reloj.instant(), contenido);
    }

    /** Sin el contenido, para que nunca llegue al registro del proceso (FR-006). */
    @Override
    public String toString() {
        return "EventoEntreProcesos[idEvento="
                + idEvento
                + ", tipo="
                + tipo
                + ", origen="
                + origen
                + ", destino="
                + destino
                + ", momento="
                + momento
                + "]";
    }

    private static void validar(String campo, String valor, Pattern patron) {
        if (valor == null || !patron.matcher(valor).matches()) {
            throw new IllegalArgumentException(
                    "El " + campo + " del evento no tiene la forma " + patron.pattern());
        }
    }
}
