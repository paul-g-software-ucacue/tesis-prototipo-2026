package ec.mdd.validacionocs.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
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
 */
public record Transicion(
        TipoDeTransicion tipo,
        TipoDeEtapa desde,
        TipoDeEtapa hacia,
        Optional<String> carrera,
        int version,
        String hash,
        String quien,
        Instant momento) {

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
    }
}
