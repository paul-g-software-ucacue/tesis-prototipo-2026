package ec.mdd.validacionocs.dominio;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Una transición del flujo del distributivo (modelo de dominio §5, RF-34): desde qué estado, hacia
 * cuál, quién, cuándo, la versión y el hash del contenido aprobado. No cambia nunca, y también va a
 * la bitácora (RF-44, D-95).
 *
 * @param tipo un envío de parcial o un paso de etapa
 * @param desde la etapa de partida
 * @param hacia la etapa de llegada; en un envío, la misma
 * @param carrera la carrera del parcial enviado; vacía en un paso de etapa
 * @param version el número de la versión vigente del distributivo
 * @param hash el SHA-256 del contenido aprobado, en hexadecimal
 * @param quien el usuario que la hizo
 * @param momento cuándo
 * @param observaciones en una devolución, la observación de cada carrera devuelta, por carrera;
 *     vacío en los demás tipos (F12-H05)
 */
public record Transicion(
        TipoDeTransicion tipo,
        TipoDeEtapa desde,
        TipoDeEtapa hacia,
        Optional<String> carrera,
        int version,
        String hash,
        String quien,
        Instant momento,
        Map<String, Observacion> observaciones) {

    private static final Pattern HASH = Pattern.compile("^[0-9a-f]{64}$");

    /** Exige los datos, un hash SHA-256 en hexadecimal y una versión desde 1. */
    public Transicion {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(desde, "desde");
        Objects.requireNonNull(hacia, "hacia");
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(quien, "quien");
        Objects.requireNonNull(momento, "momento");
        if (version < 1) {
            throw new IllegalArgumentException("La versión empieza en 1");
        }
        if (hash == null || !HASH.matcher(hash).matches()) {
            throw new IllegalArgumentException("El hash no es un SHA-256 en hexadecimal");
        }
        observaciones = Collections.unmodifiableMap(new TreeMap<>(observaciones));
        if ((tipo == TipoDeTransicion.DEVOLUCION) == observaciones.isEmpty()) {
            throw new IllegalArgumentException(
                    "Solo una devolución lleva observaciones, y siempre");
        }
    }

    /** Un envío o un paso de etapa: sin observaciones. */
    public Transicion(
            TipoDeTransicion tipo,
            TipoDeEtapa desde,
            TipoDeEtapa hacia,
            Optional<String> carrera,
            int version,
            String hash,
            String quien,
            Instant momento) {
        this(tipo, desde, hacia, carrera, version, hash, quien, momento, Map.of());
    }
}
