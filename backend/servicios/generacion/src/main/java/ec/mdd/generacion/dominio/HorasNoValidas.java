package ec.mdd.generacion.dominio;

import java.io.Serial;

/** Las horas de una actividad no son mayores que 0, pasan de 40 o tienen más de dos decimales. */
public final class HorasNoValidas extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
