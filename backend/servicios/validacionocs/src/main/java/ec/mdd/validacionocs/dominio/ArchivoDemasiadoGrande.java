package ec.mdd.validacionocs.dominio;

import java.io.Serial;

/** El archivo pasa de 1 MB. */
public final class ArchivoDemasiadoGrande extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;
}
