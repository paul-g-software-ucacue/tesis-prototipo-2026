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
 * @param rechazo en un rechazo en firma, quién rechazó, en qué paso y su observación; vacío en los
 *     demás tipos (F14-H04)
 * @param resolucion en el paso de EN_OCS a APROBADO, la resolución del OCS; vacío en los demás
 *     (F15-H01)
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
        Map<String, Observacion> observaciones,
        Optional<RechazoDelFirmante> rechazo,
        Optional<ResolucionOcs> resolucion,
        Optional<Reforma> reforma) {

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
        Objects.requireNonNull(rechazo, "rechazo");
        if ((tipo == TipoDeTransicion.RECHAZO_EN_FIRMA) != rechazo.isPresent()) {
            throw new IllegalArgumentException(
                    "Solo un rechazo en firma lleva su rechazo, y siempre");
        }
        Objects.requireNonNull(resolucion, "resolucion");
        boolean aAprobado =
                tipo == TipoDeTransicion.PASO_DE_ETAPA
                        && desde == TipoDeEtapa.EN_OCS
                        && hacia == TipoDeEtapa.APROBADO;
        if (aAprobado != resolucion.isPresent()) {
            throw new IllegalArgumentException(
                    "Solo el paso de EN_OCS a APROBADO lleva la resolución del OCS, y siempre");
        }
        Objects.requireNonNull(reforma, "reforma");
        if ((tipo == TipoDeTransicion.REFORMA) != reforma.isPresent()) {
            throw new IllegalArgumentException("Solo una reforma lleva su reforma, y siempre");
        }
    }

    /** Una transición que no es una reforma (F18-H02): todas las de antes. */
    public Transicion(
            TipoDeTransicion tipo,
            TipoDeEtapa desde,
            TipoDeEtapa hacia,
            Optional<String> carrera,
            int version,
            String hash,
            String quien,
            Instant momento,
            Map<String, Observacion> observaciones,
            Optional<RechazoDelFirmante> rechazo,
            Optional<ResolucionOcs> resolucion) {
        this(
                tipo,
                desde,
                hacia,
                carrera,
                version,
                hash,
                quien,
                momento,
                observaciones,
                rechazo,
                resolucion,
                Optional.empty());
    }

    /** Una transición sin resolución del OCS: cualquiera menos el paso de EN_OCS a APROBADO. */
    public Transicion(
            TipoDeTransicion tipo,
            TipoDeEtapa desde,
            TipoDeEtapa hacia,
            Optional<String> carrera,
            int version,
            String hash,
            String quien,
            Instant momento,
            Map<String, Observacion> observaciones,
            Optional<RechazoDelFirmante> rechazo) {
        this(
                tipo,
                desde,
                hacia,
                carrera,
                version,
                hash,
                quien,
                momento,
                observaciones,
                rechazo,
                Optional.empty());
    }

    /** Una transición sin rechazo en firma: un envío, un paso de etapa o una devolución. */
    public Transicion(
            TipoDeTransicion tipo,
            TipoDeEtapa desde,
            TipoDeEtapa hacia,
            Optional<String> carrera,
            int version,
            String hash,
            String quien,
            Instant momento,
            Map<String, Observacion> observaciones) {
        this(
                tipo,
                desde,
                hacia,
                carrera,
                version,
                hash,
                quien,
                momento,
                observaciones,
                Optional.empty());
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
