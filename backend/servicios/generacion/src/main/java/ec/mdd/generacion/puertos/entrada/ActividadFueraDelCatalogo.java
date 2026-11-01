package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/**
 * La actividad no está en el catálogo (RN-22). Responde {@code
 * GENERACION_ACTIVIDAD_FUERA_DEL_CATALOGO}.
 */
public final class ActividadFueraDelCatalogo extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
