package ec.mdd.plataforma.eventos;

import java.io.Serial;

/**
 * Otro proceso falló, no se pudo conectar con él o Usuarios no emitió el token de proceso: responde
 * 502 {@code COMUN_PROCESO_CON_ERROR} (ADR-023).
 *
 * <p>Su mensaje dice el estado HTTP o la clase de la causa; nunca la ruta, que puede llevar una
 * identificación, ni la respuesta, la clave del proceso o el token.
 */
public final class ProcesoConError extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** El fallo, con un motivo seguro para el registro. */
    public ProcesoConError(String motivo) {
        super(motivo);
    }
}
