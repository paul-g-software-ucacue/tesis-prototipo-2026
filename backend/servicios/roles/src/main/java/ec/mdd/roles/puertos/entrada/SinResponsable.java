package ec.mdd.roles.puertos.entrada;

import java.io.Serial;

/**
 * El periodo no tiene Responsable de consolidación designado (ADR-003, F13-H02). Responde {@code
 * ROLES_SIN_RESPONSABLE}.
 */
public final class SinResponsable extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public SinResponsable() {
        super("SinResponsable");
    }
}
