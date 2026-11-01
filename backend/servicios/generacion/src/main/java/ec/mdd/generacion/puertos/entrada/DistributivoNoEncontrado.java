package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/** El periodo no tiene distributivo. Responde {@code GENERACION_DISTRIBUTIVO_NO_ENCONTRADO}. */
public final class DistributivoNoEncontrado extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
