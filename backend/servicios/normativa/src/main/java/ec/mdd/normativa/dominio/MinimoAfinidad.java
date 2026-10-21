package ec.mdd.normativa.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * El puntaje de afinidad desde el cual se sugiere un docente sin justificación (RN-19, glosario
 * §7.3). Es el parámetro {@code minimo_afinidad} de la regla RN-19 de la versión; asignar a un
 * docente por debajo exige justificación, salvo en las asignaturas de tronco común (Distributivo,
 * MD-02).
 *
 * @param valor el puntaje mínimo
 */
public record MinimoAfinidad(BigDecimal valor) {

    /** La regla que lo da. */
    public static final String REGLA = "RN-19";

    /** Exige el valor. */
    public MinimoAfinidad {
        Objects.requireNonNull(valor, "valor");
    }

    /**
     * El mínimo de la regla RN-19.
     *
     * @throws IllegalArgumentException si la regla no es RN-19 o no tiene el parámetro
     */
    public static MinimoAfinidad de(Regla regla) {
        if (!REGLA.equals(regla.identificador())) {
            throw new IllegalArgumentException(
                    "El mínimo de afinidad es de " + REGLA + ", no de " + regla.identificador());
        }
        return new MinimoAfinidad(
                regla.parametro("minimo_afinidad")
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "A " + REGLA + " le falta minimo_afinidad")));
    }
}
