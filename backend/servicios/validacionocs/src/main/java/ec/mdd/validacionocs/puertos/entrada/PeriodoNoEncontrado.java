package ec.mdd.validacionocs.puertos.entrada;

import java.io.Serial;

/** El periodo no está en los ciclos académicos. */
public final class PeriodoNoEncontrado extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
