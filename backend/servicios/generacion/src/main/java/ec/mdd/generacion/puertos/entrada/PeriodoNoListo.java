package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/** El periodo no está listo para su distributivo. Responde {@code GENERACION_PERIODO_NO_LISTO}. */
public final class PeriodoNoListo extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
