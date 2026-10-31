package ec.mdd.plataforma.errores.nucleo;

import java.io.Serial;

/**
 * Quien solo tiene «lo propio» sobre la oferta no tiene carrera indicada: un dato sin configurar no
 * abre todo (D-64). Responde {@code COMUN_CARRERA_SIN_INDICAR}.
 */
public final class CarreraSinIndicar extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
