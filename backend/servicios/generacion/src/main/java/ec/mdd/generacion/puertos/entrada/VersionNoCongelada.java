package ec.mdd.generacion.puertos.entrada;

import java.io.Serial;

/**
 * Se pidió el contenido congelado de un periodo cuya versión vigente no está congelada (F13-H02).
 * Solo lo pide Firma después de entrar en firma, así que no llega a la API HTTP.
 */
public final class VersionNoCongelada extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** Sin datos. */
    public VersionNoCongelada() {
        super("VersionNoCongelada");
    }
}
