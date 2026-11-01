package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/** No se puede proponer sin distributivo. Responde {@code GENERACION_SIN_DISTRIBUTIVO}. */
public final class SinDistributivo extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
