package ec.mdd.firma.puertos.entrada;

import java.time.Instant;
import java.util.Objects;

/**
 * El firmante en turno rechazó en firma la versión congelada (F14-H04, D-105, glosario §6).
 *
 * <p>Firma no conoce el flujo (MD-05): solo dice quién rechazó, en qué paso y por qué. Va en la API
 * publicada, como {@link CadenaDeFirmasCompleta}, para que Aprobación lo escuche y devuelva el
 * distributivo a revisión, sin un ciclo entre los dos. Se publica en la transacción que registra el
 * rechazo y se entrega después de confirmarla, con reintento (ADR-018): puede llegar más de una
 * vez.
 *
 * @param periodo el código del periodo
 * @param version la versión congelada que se rechazó
 * @param orden el paso que se rechazó
 * @param rol el rol del paso
 * @param rechazadoPor quién rechazó
 * @param rechazadoEn cuándo
 * @param observacion qué hay que corregir
 */
public record RechazoEnFirma(
        String periodo,
        int version,
        int orden,
        String rol,
        String rechazadoPor,
        Instant rechazadoEn,
        String observacion) {

    /** Exige los datos. */
    public RechazoEnFirma {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(rechazadoPor, "rechazadoPor");
        Objects.requireNonNull(rechazadoEn, "rechazadoEn");
        Objects.requireNonNull(observacion, "observacion");
        if (version < 1 || orden < 1) {
            throw new IllegalArgumentException("la versión y el paso empiezan en 1");
        }
    }
}
