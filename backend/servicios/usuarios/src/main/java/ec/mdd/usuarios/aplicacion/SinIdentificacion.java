package ec.mdd.usuarios.aplicacion;

import java.io.Serial;

/**
 * La cuenta no existe o no tiene número de identificación (F14-H01, D-102). Responde {@code
 * USUARIOS_SIN_IDENTIFICACION}, igual en los dos casos.
 */
public final class SinIdentificacion extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public SinIdentificacion() {
        super("SinIdentificacion");
    }
}
