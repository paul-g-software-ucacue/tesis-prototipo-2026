package ec.mdd.validacionocs.dominio;

/**
 * EN_CONSOLIDACION no admite devolución en la copia de la configuración del distributivo (F12-H05).
 * Responde {@code VALIDACIONOCS_DEVOLUCION_DESACTIVADA}.
 */
public final class DevolucionDesactivada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public DevolucionDesactivada() {
        super("DevolucionDesactivada");
    }
}
