package ec.mdd.generacion.dominio;

import java.io.Serial;

/**
 * El docente no tiene el cargo que exige la actividad. Responde {@code
 * GENERACION_CARGO_NO_VIGENTE}.
 */
public final class CargoNoVigente extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
