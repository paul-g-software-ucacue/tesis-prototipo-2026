package ec.mdd.docentes.dominio;

import java.math.BigDecimal;

/**
 * La regla de las horas semanales de un proyecto y de sus participantes (carga 9, aclaración 7 de
 * F03-H05): mayores que 0, con hasta dos decimales y hasta 5 caracteres. Los caracteres se cuentan
 * sin ceros de más: {@code 2.50} cuenta como {@code 2.5}.
 */
final class HorasSemanales {

    /** El largo máximo, como en el diccionario. */
    static final int CARACTERES = 5;

    private static final BigDecimal MAXIMO = new BigDecimal("1000");

    private HorasSemanales() {}

    /**
     * @return las horas, sin ceros de más
     * @throws DatoInvalido en el campo si no cumplen la regla
     */
    static BigDecimal verificar(BigDecimal horas, String campo) {
        if (horas == null) {
            throw new DatoInvalido(campo, "Indica las horas semanales");
        }
        if (horas.signum() <= 0) {
            throw new DatoInvalido(campo, "Las horas semanales tienen que ser mayores que 0");
        }
        if (horas.compareTo(MAXIMO) >= 0) {
            throw new DatoInvalido(campo, "Las horas semanales tienen que ser menores que 1000");
        }
        BigDecimal limpias = horas.stripTrailingZeros();
        if (limpias.scale() > 2) {
            throw new DatoInvalido(campo, "Las horas semanales admiten hasta dos decimales");
        }
        if (limpias.toPlainString().length() > CARACTERES) {
            throw new DatoInvalido(
                    campo,
                    "Las horas semanales tienen hasta "
                            + CARACTERES
                            + " caracteres, como 999.9 o 99.75");
        }
        return limpias.scale() < 0 ? limpias.setScale(0) : limpias;
    }
}
