package ec.mdd.firma.dominio;

import java.io.Serial;

/** El PDF subido no tiene bytes ({@code FIRMA_ARCHIVO_VACIO}, 400). Sin resultado. */
public final class ArchivoVacio extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** Sin datos: no hay nada que registrar. */
    public ArchivoVacio() {
        super("PDF firmado vacío");
    }
}
