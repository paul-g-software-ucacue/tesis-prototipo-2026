package ec.mdd.validacionocs.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * Quién rechazó en firma, en qué paso y por qué (F14-H04, glosario «Rechazo en firma», D-105). Lo
 * avisa Firma; la transición la registra el sistema, así que el firmante va aquí.
 *
 * @param orden el paso que se rechazó
 * @param rol el rol del paso
 * @param rechazadoPor el usuario del firmante en turno
 * @param rechazadoEn cuándo rechazó
 * @param observacion qué hay que corregir (RF-36)
 */
public record RechazoDelFirmante(
        int orden, String rol, String rechazadoPor, Instant rechazadoEn, Observacion observacion) {

    /** Exige los datos y un paso desde 1. */
    public RechazoDelFirmante {
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(rechazadoPor, "rechazadoPor");
        Objects.requireNonNull(rechazadoEn, "rechazadoEn");
        Objects.requireNonNull(observacion, "observacion");
        if (orden < 1) {
            throw new IllegalArgumentException("El paso empieza en 1");
        }
    }
}
