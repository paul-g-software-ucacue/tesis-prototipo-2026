package ec.mdd.firma.dominio;

import java.io.Serial;

/**
 * El paso en turno no tiene guardado el nombre que imprime el PDF ({@code FIRMA_PASO_SIN_NOMBRE},
 * 409): el documento se generó antes de F14-H03 (D-104). No se acepta una firma que no se puede
 * comprobar.
 */
public final class PasoSinNombre extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** Sin datos: el documento y el paso ya se conocen por la petición. */
    public PasoSinNombre() {
        super("El paso en turno no tiene nombre impreso guardado");
    }
}
