package ec.mdd.validacionocs.dominio;

/** Error del envío a revisión (F10-H03, D-97); lo traduce el catálogo de errores. */
public final class SinInforme extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public SinInforme() {
        super("SinInforme");
    }
}
