package ec.mdd.generacion.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * Por qué se acepta una inconsistencia global blanda (RF-29, MD-07). Es distinta de la {@link
 * Justificacion} de una propuesta, que dice por qué se propuso a un docente.
 *
 * @param tipo el tipo de la inconsistencia
 * @param objeto la clave de lo que la incumple
 * @param texto el motivo
 * @param por quién la escribió
 * @param en cuándo
 */
public record JustificacionDeInconsistencia(
        TipoDeInconsistencia tipo, String objeto, String texto, String por, Instant en) {

    private static final int LARGO_MINIMO = 10;
    private static final int LARGO_MAXIMO = 500;

    /** Exige los datos. */
    public JustificacionDeInconsistencia {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(objeto, "objeto");
        Objects.requireNonNull(texto, "texto");
        Objects.requireNonNull(por, "por");
        Objects.requireNonNull(en, "en");
    }

    /**
     * Una justificación escrita ahora, con el texto sin los espacios de los extremos.
     *
     * @param tipo el tipo de la inconsistencia
     * @param objeto la clave de lo que la incumple
     * @param texto el motivo
     * @param por quién la escribe
     * @param en cuándo
     * @return la justificación
     * @throws JustificacionObligatoria si el texto no tiene de 10 a 500 caracteres
     */
    public static JustificacionDeInconsistencia nueva(
            TipoDeInconsistencia tipo, String objeto, String texto, String por, Instant en) {
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.length() < LARGO_MINIMO || limpio.length() > LARGO_MAXIMO) {
            throw new JustificacionObligatoria();
        }
        return new JustificacionDeInconsistencia(tipo, objeto, limpio, por, en);
    }
}
