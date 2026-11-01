package ec.mdd.generacion.dominio;

import java.io.Serial;

/**
 * El parcial se envió, o la versión ya no está editable. Responde {@code
 * GENERACION_PARCIAL_NO_EDITABLE}.
 */
public final class ParcialNoEditable extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
