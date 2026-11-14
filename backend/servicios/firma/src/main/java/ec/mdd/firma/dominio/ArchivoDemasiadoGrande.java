package ec.mdd.firma.dominio;

import java.io.Serial;

/**
 * El PDF subido pasa de 10 MB ({@code FIRMA_ARCHIVO_DEMASIADO_GRANDE}, 413). Se rechaza antes de
 * validar, sin resultado. El PDF de 165 paralelos pesa cientos de KB, y cada firma de FirmaEC
 * agrega unos 32 KB (D-46).
 */
public final class ArchivoDemasiadoGrande extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** El tamaño máximo de un PDF firmado. El proceso corta a 11 MB. */
    public static final int LIMITE_EN_BYTES = 10 * 1024 * 1024;

    /** Recibe el tamaño recibido, que solo va al registro. */
    public ArchivoDemasiadoGrande(long tamano) {
        super("PDF firmado de " + tamano + " bytes");
    }
}
