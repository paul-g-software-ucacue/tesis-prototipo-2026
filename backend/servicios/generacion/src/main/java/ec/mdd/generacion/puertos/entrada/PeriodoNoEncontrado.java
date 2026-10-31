package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/** El periodo no existe en Académico. Responde {@code GENERACION_PERIODO_NO_ENCONTRADO}. */
public final class PeriodoNoEncontrado extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
