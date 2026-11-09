package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/**
 * Consolidar, justificar o adjuntar el informe con el distributivo ya en revisión del
 * Vicerrectorado o después (#215, D-99). Responde {@code GENERACION_CONSOLIDADO_EN_REVISION}.
 */
public final class ConsolidadoEnRevision extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
