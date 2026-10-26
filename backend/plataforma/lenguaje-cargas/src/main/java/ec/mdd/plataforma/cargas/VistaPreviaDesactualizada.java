package ec.mdd.plataforma.cargas;

import java.io.Serial;

/**
 * La vista previa cambió entre la revisión y la confirmación, porque alguien cambió los datos del
 * dueño: no se aplica nada (FR-006).
 */
public class VistaPreviaDesactualizada extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** Crea la excepción. */
    public VistaPreviaDesactualizada() {
        super("La vista previa cambió desde que se revisó");
    }
}
