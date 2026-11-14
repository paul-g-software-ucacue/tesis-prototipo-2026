package ec.mdd.firma.dominio;

import java.io.Serial;

/**
 * El reintento de generar el documento firmable falló (F13-H02, FR-012). Responde {@code
 * FIRMA_GENERACION_FALLIDA}, con el motivo en los detalles.
 */
public final class GeneracionFallida extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    private final String motivo;

    /** Recibe el motivo, el mismo que queda registrado en la generación. */
    public GeneracionFallida(String motivo) {
        super("GeneracionFallida");
        this.motivo = motivo;
    }

    /** Por qué falló. */
    public String motivo() {
        return motivo;
    }
}
