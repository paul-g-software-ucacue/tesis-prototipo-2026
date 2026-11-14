package ec.mdd.firma.dominio;

import java.io.Serial;

/** Esqueleto (F14-H02, T006). */
public final class ArchivoDemasiadoGrande extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    public static final int LIMITE_EN_BYTES = 0;
}
