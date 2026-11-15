package ec.mdd.firma.dominio;

import java.io.Serial;

/**
 * La observación de un rechazo en firma no tiene, sin los espacios de los extremos, de 10 a 500
 * caracteres ({@code FIRMA_OBSERVACION_INVALIDA}, 400; RF-36, D-105). El rechazo no se registra.
 */
public final class ObservacionInvalida extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo dice el largo admitido. */
    public ObservacionInvalida() {
        super("La observación del rechazo no tiene de 10 a 500 caracteres");
    }
}
