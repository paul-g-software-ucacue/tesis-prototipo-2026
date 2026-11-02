package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/** Adjuntar el informe de un periodo sin consolidar. Responde {@code GENERACION_SIN_CONSOLIDAR}. */
public final class SinConsolidar extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
