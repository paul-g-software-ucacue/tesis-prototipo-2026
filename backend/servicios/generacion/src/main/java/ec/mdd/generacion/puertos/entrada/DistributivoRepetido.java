package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/** El periodo ya tiene distributivo. Responde {@code GENERACION_DISTRIBUTIVO_REPETIDO}. */
public final class DistributivoRepetido extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
