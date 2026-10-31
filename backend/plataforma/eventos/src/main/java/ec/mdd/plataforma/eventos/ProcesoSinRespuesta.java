package ec.mdd.plataforma.eventos;

import java.io.Serial;

/**
 * Otro proceso no respondió dentro del plazo: responde 504 {@code COMUN_PROCESO_SIN_RESPUESTA}
 * (ADR-009, ADR-023). Su mensaje no lleva la ruta, como el de {@link ProcesoConError}.
 */
public final class ProcesoSinRespuesta extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** El plazo agotado. */
    public ProcesoSinRespuesta() {
        super("otro proceso no respondió dentro del plazo");
    }
}
