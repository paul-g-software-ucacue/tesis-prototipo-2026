package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/** El periodo está cerrado. Responde {@code GENERACION_PERIODO_CERRADO}. */
public final class PeriodoCerrado extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
