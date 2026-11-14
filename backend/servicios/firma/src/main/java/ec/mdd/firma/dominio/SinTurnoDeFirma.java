package ec.mdd.firma.dominio;

import java.io.Serial;

/**
 * Quien pide el archivo no es el firmante esperado del paso en turno, o el documento no existe o no
 * está generado (F14-H01, FR-010). El controlador la traduce al 403 común, sin decir cuál de los
 * casos es.
 */
public final class SinTurnoDeFirma extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** Sin datos: el 403 común no dice por qué. */
    public SinTurnoDeFirma() {
        super("SinTurnoDeFirma");
    }
}
