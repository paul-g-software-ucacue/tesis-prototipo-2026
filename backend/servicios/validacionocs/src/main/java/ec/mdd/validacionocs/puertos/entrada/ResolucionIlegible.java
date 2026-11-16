package ec.mdd.validacionocs.puertos.entrada;

/**
 * El PDF de la resolución falta en el almacén o no coincide con su huella (F15-H01): nunca se
 * entrega un archivo dañado. Lo traduce el catálogo de errores.
 */
public final class ResolucionIlegible extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public ResolucionIlegible() {
        super("ResolucionIlegible");
    }
}
