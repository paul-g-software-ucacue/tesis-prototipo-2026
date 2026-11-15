package ec.mdd.validacionocs.puertos.entrada;

/**
 * Descargar la resolución de un periodo que todavía no la tiene (F15-H01). Lo traduce el catálogo
 * de errores.
 */
public final class ResolucionNoEncontrada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public ResolucionNoEncontrada() {
        super("ResolucionNoEncontrada");
    }
}
