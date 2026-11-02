package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/**
 * Justificar una inconsistencia que ya no está en el panel. Responde {@code
 * GENERACION_INCONSISTENCIA_NO_ENCONTRADA}.
 */
public final class InconsistenciaNoEncontrada extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
