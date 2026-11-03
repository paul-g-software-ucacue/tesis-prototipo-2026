package ec.mdd.validacionocs.puertos.entrada;

import java.io.Serial;

/** El periodo ya no está en Planificación. */
public final class PeriodoSinPlanificacion extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
