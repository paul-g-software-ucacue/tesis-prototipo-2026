package ec.mdd.firma.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * El rechazo en firma de un documento (F14-H04, RF-36, D-105): el firmante en turno no firmó y
 * escribió una observación. Es el término «Rechazo en firma» del glosario; el aviso a Aprobación es
 * {@code RechazoEnFirma}. Un documento tiene a lo sumo uno, y con él nadie queda en turno.
 *
 * <p>La observación se guarda sin los espacios de los extremos y debe tener de 10 a 500 caracteres,
 * como la de una devolución (D-98).
 *
 * @param orden el paso en turno al rechazar
 * @param rechazadoPor el usuario del firmante en turno
 * @param rechazadoEn cuándo
 * @param observacion qué hay que corregir
 * @throws ObservacionInvalida si la observación falta o no tiene de 10 a 500 caracteres
 */
public record RechazoDelFirmante(
        int orden, String rechazadoPor, Instant rechazadoEn, String observacion) {

    /** El largo mínimo de la observación, recortada. */
    public static final int MINIMO = 10;

    /** El largo máximo de la observación, recortada. */
    public static final int MAXIMO = 500;

    /** Exige los datos y recorta la observación. */
    public RechazoDelFirmante {
        Objects.requireNonNull(rechazadoPor, "rechazadoPor");
        Objects.requireNonNull(rechazadoEn, "rechazadoEn");
        if (orden < 1) {
            throw new IllegalArgumentException("El paso empieza en 1");
        }
        observacion = observacion == null ? "" : observacion.strip();
        if (observacion.length() < MINIMO || observacion.length() > MAXIMO) {
            throw new ObservacionInvalida();
        }
    }
}
