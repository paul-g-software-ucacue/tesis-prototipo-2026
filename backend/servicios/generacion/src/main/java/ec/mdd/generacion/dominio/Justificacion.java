package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Por qué se hizo una asignación (RNF-EQU-01, RN-19). La decide el sistema, con el puntaje
 * calculado al guardar: la pantalla no puede elegirla.
 *
 * @param tipo afinidad, disponibilidad o excepción
 * @param texto el texto de una excepción; {@code null} en los otros dos casos
 */
public record Justificacion(TipoDeJustificacion tipo, String texto) {

    private static final int LARGO_MINIMO = 10;
    private static final int LARGO_MAXIMO = 500;

    /** Exige el tipo. */
    public Justificacion {
        Objects.requireNonNull(tipo, "tipo");
    }

    /**
     * La justificación de una propuesta: «afinidad» si alcanza el mínimo; «disponibilidad» si no lo
     * alcanza en tronco común, que está exento; y «excepción», con su texto, si no.
     *
     * @param puntaje el puntaje de afinidad del docente
     * @param minimo el mínimo de la versión de normativa
     * @param troncoComun si la asignatura es de tronco común
     * @param texto el texto de quien propone; solo se conserva en una excepción
     * @throws JustificacionObligatoria si es una excepción sin un texto de 10 a 500 caracteres
     */
    public static Justificacion para(
            BigDecimal puntaje, BigDecimal minimo, boolean troncoComun, String texto) {
        if (puntaje.compareTo(minimo) >= 0) {
            return new Justificacion(TipoDeJustificacion.AFINIDAD, null);
        }
        if (troncoComun) {
            return new Justificacion(TipoDeJustificacion.DISPONIBILIDAD, null);
        }
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.length() < LARGO_MINIMO || limpio.length() > LARGO_MAXIMO) {
            throw new JustificacionObligatoria();
        }
        return new Justificacion(TipoDeJustificacion.EXCEPCION, limpio);
    }
}
